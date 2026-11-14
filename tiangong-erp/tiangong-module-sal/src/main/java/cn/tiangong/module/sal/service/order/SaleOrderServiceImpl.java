package cn.tiangong.module.sal.service.order;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.bpm.service.ApprovalService;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOrderDO;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOrderItemDO;
import cn.tiangong.module.sal.dal.mysql.ErpSaleOrderItemMapper;
import cn.tiangong.module.sal.dal.mysql.ErpSaleOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class SaleOrderServiceImpl implements SaleOrderService {

    public static final String BIZ_TYPE = "sal_order";

    private final ErpSaleOrderMapper orderMapper;
    private final ErpSaleOrderItemMapper orderItemMapper;
    private final SerialNoApi serialNoApi;
    private final ChatterService chatterService;
    private final ApprovalService approvalService;

    @Override
    public PageResult<ErpSaleOrderDO> getOrderPage(PageParam pageParam, String no, Long customerId, Integer status) {
        LambdaQueryWrapper<ErpSaleOrderDO> wrapper = new LambdaQueryWrapper<ErpSaleOrderDO>()
                .like(no != null && !no.isBlank(), ErpSaleOrderDO::getNo, no)
                .eq(customerId != null, ErpSaleOrderDO::getCustomerId, customerId)
                .eq(status != null, ErpSaleOrderDO::getStatus, status)
                .orderByDesc(ErpSaleOrderDO::getId);
        return orderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public OrderDetail getOrderDetail(Long id) {
        ErpSaleOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw of(7_00, 1, "销售订单不存在");
        }
        return new OrderDetail(order, getItemList(id));
    }

    @Override
    public List<ErpSaleOrderItemDO> getItemList(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<ErpSaleOrderItemDO>()
                .eq(ErpSaleOrderItemDO::getOrderId, orderId)
                .orderByAsc(ErpSaleOrderItemDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long customerId, LocalDate orderDate, LocalDate expectDate,
                            String remark, List<Item> items) {
        validateItems(items);
        ErpSaleOrderDO order = new ErpSaleOrderDO();
        order.setNo(serialNoApi.nextNo("sale_order"));
        fillHead(order, customerId, orderDate, expectDate, remark, items);
        order.setStatus(BizOrderStatus.DRAFT.getValue());
        orderMapper.insert(order);
        insertItems(order.getId(), items);
        chatterService.trace(BIZ_TYPE, order.getId(), "创建销售订单 " + order.getNo(), null);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, Long customerId, LocalDate orderDate, LocalDate expectDate,
                            String remark, List<Item> items) {
        ErpSaleOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        validateItems(items);
        fillHead(order, customerId, orderDate, expectDate, remark, items);
        orderMapper.updateById(order);
        orderItemMapper.delete(new LambdaQueryWrapper<ErpSaleOrderItemDO>()
                .eq(ErpSaleOrderItemDO::getOrderId, id));
        insertItems(id, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        ErpSaleOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        orderMapper.deleteById(id);
        orderItemMapper.delete(new LambdaQueryWrapper<ErpSaleOrderItemDO>()
                .eq(ErpSaleOrderItemDO::getOrderId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOrder(Long id) {
        ErpSaleOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(),
                BizOrderStatus.DRAFT, BizOrderStatus.APPROVING);
        order.setStatus(BizOrderStatus.AUDITED.getValue());
        order.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        order.setAuditTime(LocalDateTime.now());
        orderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "审核销售订单 " + order.getNo(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        ErpSaleOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        if (!approvalService.submit("sal_order", BIZ_TYPE, id, order.getNo())) {
            throw of(7_00, 5, "销售订单未配置审批流，请直接审核");
        }
        order.setStatus(BizOrderStatus.APPROVING.getValue());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalRejected(Long id) {
        ErpSaleOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.APPROVING);
        order.setStatus(BizOrderStatus.DRAFT.getValue());
        orderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "审批驳回，退回草稿", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeposit(Long id, java.math.BigDecimal depositAmount) {
        ErpSaleOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw of(7_01, 2, "订单不存在: " + id);
        }
        order.setDepositAmount(depositAmount);
        orderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "订金更新为 " + depositAmount, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidOrder(Long id) {
        ErpSaleOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        order.setStatus(BizOrderStatus.VOIDED.getValue());
        orderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "作废销售订单 " + order.getNo(), null);
    }

    private void fillHead(ErpSaleOrderDO order, Long customerId, LocalDate orderDate,
                          LocalDate expectDate, String remark, List<Item> items) {
        if (customerId == null) {
            throw of(7_00, 2, "客户不能为空");
        }
        order.setCustomerId(customerId);
        order.setOrderDate(orderDate != null ? orderDate : LocalDate.now());
        order.setExpectDate(expectDate);
        order.setRemark(remark);
        BigDecimal totalQty = items.stream().map(Item::qty).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = items.stream()
                .map(i -> i.qty().multiply(i.price())).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setTotalTax(BigDecimal.ZERO);
        order.setTotalAmountTax(totalAmount);
    }

    private void insertItems(Long orderId, List<Item> items) {
        for (Item item : items) {
            ErpSaleOrderItemDO itemDO = new ErpSaleOrderItemDO();
            itemDO.setOrderId(orderId);
            itemDO.setProductId(item.productId());
            itemDO.setUnitId(item.unitId());
            itemDO.setQty(item.qty());
            itemDO.setPrice(item.price());
            itemDO.setAmount(item.qty().multiply(item.price()));
            itemDO.setTaxRate(item.taxRate() == null ? BigDecimal.ZERO : item.taxRate());
            itemDO.setOutboundQty(BigDecimal.ZERO);
            itemDO.setRemark(item.remark());
            orderItemMapper.insert(itemDO);
        }
    }

    private void validateItems(List<Item> items) {
        if (items == null || items.isEmpty()) {
            throw of(7_00, 3, "订单明细不能为空");
        }
        for (Item item : items) {
            if (item.productId() == null || item.qty() == null || item.qty().signum() <= 0
                    || item.price() == null || item.price().signum() < 0) {
                throw of(7_00, 4, "明细中存在产品/数量/价格不合法的行");
            }
        }
    }

    private ErpSaleOrderDO getOrder(Long id) {
        ErpSaleOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw of(7_00, 1, "销售订单不存在");
        }
        return order;
    }
}
