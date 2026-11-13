package cn.tiangong.module.pur.service.order;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.bpm.service.ApprovalService;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderDO;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderItemDO;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseOrderItemMapper;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseOrderMapper;
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
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    public static final String BIZ_TYPE = "pur_order";

    private final ErpPurchaseOrderMapper orderMapper;
    private final ErpPurchaseOrderItemMapper orderItemMapper;
    private final SerialNoApi serialNoApi;
    private final ChatterService chatterService;
    private final ApprovalService approvalService;

    @Override
    public PageResult<ErpPurchaseOrderDO> getOrderPage(PageParam pageParam, String no, Long supplierId, Integer status) {
        LambdaQueryWrapper<ErpPurchaseOrderDO> wrapper = new LambdaQueryWrapper<ErpPurchaseOrderDO>()
                .like(no != null && !no.isBlank(), ErpPurchaseOrderDO::getNo, no)
                .eq(supplierId != null, ErpPurchaseOrderDO::getSupplierId, supplierId)
                .eq(status != null, ErpPurchaseOrderDO::getStatus, status)
                .orderByDesc(ErpPurchaseOrderDO::getId);
        return orderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public OrderDetail getOrderDetail(Long id) {
        ErpPurchaseOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw of(6_00, 1, "采购订单不存在");
        }
        return new OrderDetail(order, getItemList(id));
    }

    @Override
    public List<ErpPurchaseOrderItemDO> getItemList(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<ErpPurchaseOrderItemDO>()
                .eq(ErpPurchaseOrderItemDO::getOrderId, orderId)
                .orderByAsc(ErpPurchaseOrderItemDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long supplierId, LocalDate orderDate, LocalDate expectDate,
                            String remark, List<Item> items) {
        validateItems(items);
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO();
        order.setNo(serialNoApi.nextNo("purchase_order"));
        fillHead(order, supplierId, orderDate, expectDate, remark, items);
        order.setStatus(BizOrderStatus.DRAFT.getValue());
        orderMapper.insert(order);
        insertItems(order.getId(), items);
        chatterService.trace(BIZ_TYPE, order.getId(), "创建采购订单 " + order.getNo(), null);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, Long supplierId, LocalDate orderDate, LocalDate expectDate,
                            String remark, List<Item> items) {
        ErpPurchaseOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        validateItems(items);
        fillHead(order, supplierId, orderDate, expectDate, remark, items);
        orderMapper.updateById(order);
        orderItemMapper.delete(new LambdaQueryWrapper<ErpPurchaseOrderItemDO>()
                .eq(ErpPurchaseOrderItemDO::getOrderId, id));
        insertItems(id, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        ErpPurchaseOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        orderMapper.deleteById(id);
        orderItemMapper.delete(new LambdaQueryWrapper<ErpPurchaseOrderItemDO>()
                .eq(ErpPurchaseOrderItemDO::getOrderId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOrder(Long id) {
        ErpPurchaseOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(),
                BizOrderStatus.DRAFT, BizOrderStatus.APPROVING);
        order.setStatus(BizOrderStatus.AUDITED.getValue());
        order.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        order.setAuditTime(LocalDateTime.now());
        orderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "审核采购订单 " + order.getNo(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        ErpPurchaseOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        if (!approvalService.submit("pur_order", BIZ_TYPE, id, order.getNo())) {
            throw of(6_00, 5, "采购订单未配置审批流，请直接审核");
        }
        order.setStatus(BizOrderStatus.APPROVING.getValue());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalRejected(Long id) {
        ErpPurchaseOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.APPROVING);
        order.setStatus(BizOrderStatus.DRAFT.getValue());
        orderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "审批驳回，退回草稿", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidOrder(Long id) {
        ErpPurchaseOrderDO order = getOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        order.setStatus(BizOrderStatus.VOIDED.getValue());
        orderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "作废采购订单 " + order.getNo(), null);
    }

    private void fillHead(ErpPurchaseOrderDO order, Long supplierId, LocalDate orderDate,
                          LocalDate expectDate, String remark, List<Item> items) {
        if (supplierId == null) {
            throw of(6_00, 2, "供应商不能为空");
        }
        order.setSupplierId(supplierId);
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
            ErpPurchaseOrderItemDO itemDO = new ErpPurchaseOrderItemDO();
            itemDO.setOrderId(orderId);
            itemDO.setProductId(item.productId());
            itemDO.setUnitId(item.unitId());
            itemDO.setQty(item.qty());
            itemDO.setPrice(item.price());
            itemDO.setAmount(item.qty().multiply(item.price()));
            itemDO.setTaxRate(item.taxRate() == null ? BigDecimal.ZERO : item.taxRate());
            itemDO.setInboundQty(BigDecimal.ZERO);
            itemDO.setRemark(item.remark());
            orderItemMapper.insert(itemDO);
        }
    }

    private void validateItems(List<Item> items) {
        if (items == null || items.isEmpty()) {
            throw of(6_00, 3, "订单明细不能为空");
        }
        for (Item item : items) {
            if (item.productId() == null || item.qty() == null || item.qty().signum() <= 0
                    || item.price() == null || item.price().signum() < 0) {
                throw of(6_00, 4, "明细中存在产品/数量/价格不合法的行");
            }
        }
    }

    private ErpPurchaseOrderDO getOrder(Long id) {
        ErpPurchaseOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw of(6_00, 1, "采购订单不存在");
        }
        return order;
    }
}
