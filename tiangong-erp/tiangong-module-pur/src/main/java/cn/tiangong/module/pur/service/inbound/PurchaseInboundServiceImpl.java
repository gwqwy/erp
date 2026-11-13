package cn.tiangong.module.pur.service.inbound;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.bpm.service.ApprovalService;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseInboundDO;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseInboundItemDO;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderDO;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderItemDO;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseInboundItemMapper;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseInboundMapper;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseOrderItemMapper;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseOrderMapper;
import cn.tiangong.module.pur.service.order.PurchaseOrderServiceImpl;
import cn.tiangong.module.fin.api.FinApi;
import cn.tiangong.module.fin.api.dto.FinanceEvent;
import cn.tiangong.module.stk.api.StockPostingApi;
import cn.tiangong.module.stk.api.dto.MoveCommand;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class PurchaseInboundServiceImpl implements PurchaseInboundService {

    public static final String BIZ_TYPE = "pur_inbound";
    private static final String MOVE_BIZ_TYPE = "purchase_in";
    /** 60 = 已冲销（红字） */
    public static final int STATUS_REVERSED = 60;

    private final ErpPurchaseInboundMapper inboundMapper;
    private final ErpPurchaseInboundItemMapper inboundItemMapper;
    private final ErpPurchaseOrderMapper orderMapper;
    private final ErpPurchaseOrderItemMapper orderItemMapper;
    private final SerialNoApi serialNoApi;
    private final StockPostingApi stockPostingApi;
    private final ChatterService chatterService;
    private final ApplicationEventPublisher eventPublisher;
    private final FinApi finApi;
    private final ApprovalService approvalService;

    @Override
    public PageResult<ErpPurchaseInboundDO> getInboundPage(PageParam pageParam, String no, Long supplierId,
                                                           Long orderId, Integer status) {
        LambdaQueryWrapper<ErpPurchaseInboundDO> wrapper = new LambdaQueryWrapper<ErpPurchaseInboundDO>()
                .like(no != null && !no.isBlank(), ErpPurchaseInboundDO::getNo, no)
                .eq(supplierId != null, ErpPurchaseInboundDO::getSupplierId, supplierId)
                .eq(orderId != null, ErpPurchaseInboundDO::getOrderId, orderId)
                .eq(status != null, ErpPurchaseInboundDO::getStatus, status)
                .orderByDesc(ErpPurchaseInboundDO::getId);
        return inboundMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public InboundDetail getInboundDetail(Long id) {
        ErpPurchaseInboundDO inbound = getInbound(id);
        List<ErpPurchaseInboundItemDO> items = inboundItemMapper.selectList(
                new LambdaQueryWrapper<ErpPurchaseInboundItemDO>()
                        .eq(ErpPurchaseInboundItemDO::getInboundId, id)
                        .orderByAsc(ErpPurchaseInboundItemDO::getId));
        return new InboundDetail(inbound, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInbound(Long supplierId, Long warehouseId, Long orderId, LocalDate inboundDate,
                              String remark, List<Item> items) {
        validate(supplierId, warehouseId, items);
        if (orderId != null) {
            ErpPurchaseOrderDO order = orderMapper.selectById(orderId);
            if (order == null) {
                throw of(6_01, 1, "来源采购订单不存在");
            }
            OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.AUDITED);
        }
        ErpPurchaseInboundDO inbound = new ErpPurchaseInboundDO();
        inbound.setNo(serialNoApi.nextNo("purchase_inbound"));
        inbound.setSupplierId(supplierId);
        inbound.setWarehouseId(warehouseId);
        inbound.setOrderId(orderId);
        inbound.setInboundDate(inboundDate != null ? inboundDate : LocalDate.now());
        inbound.setRemark(remark);
        inbound.setTotalQty(items.stream().map(Item::qty).reduce(BigDecimal.ZERO, BigDecimal::add));
        inbound.setTotalAmount(items.stream()
                .map(i -> i.qty().multiply(i.price())).reduce(BigDecimal.ZERO, BigDecimal::add));
        inbound.setStatus(BizOrderStatus.DRAFT.getValue());
        inboundMapper.insert(inbound);
        for (Item item : items) {
            ErpPurchaseInboundItemDO itemDO = new ErpPurchaseInboundItemDO();
            itemDO.setInboundId(inbound.getId());
            itemDO.setOrderItemId(item.orderItemId());
            itemDO.setProductId(item.productId());
            itemDO.setUnitId(item.unitId());
            itemDO.setQty(item.qty());
            itemDO.setPrice(item.price());
            itemDO.setAmount(item.qty().multiply(item.price()));
            itemDO.setRemark(item.remark());
            inboundItemMapper.insert(itemDO);
        }
        chatterService.trace(BIZ_TYPE, inbound.getId(), "创建采购入库单 " + inbound.getNo(), null);
        return inbound.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInbound(Long id) {
        ErpPurchaseInboundDO inbound = getInbound(id);
        OrderStateChecker.require(inbound.getStatus(), inbound.getNo(), BizOrderStatus.DRAFT);
        inboundMapper.deleteById(id);
        inboundItemMapper.delete(new LambdaQueryWrapper<ErpPurchaseInboundItemDO>()
                .eq(ErpPurchaseInboundItemDO::getInboundId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        ErpPurchaseInboundDO inbound = getInbound(id);
        OrderStateChecker.require(inbound.getStatus(), inbound.getNo(), BizOrderStatus.DRAFT);
        if (!approvalService.submit("pur_inbound", BIZ_TYPE, id, inbound.getNo())) {
            throw of(6_01, 7, "采购入库未配置审批流，请直接审核");
        }
        inbound.setStatus(BizOrderStatus.APPROVING.getValue());
        inboundMapper.updateById(inbound);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalRejected(Long id) {
        ErpPurchaseInboundDO inbound = getInbound(id);
        OrderStateChecker.require(inbound.getStatus(), inbound.getNo(), BizOrderStatus.APPROVING);
        inbound.setStatus(BizOrderStatus.DRAFT.getValue());
        inboundMapper.updateById(inbound);
        chatterService.trace(BIZ_TYPE, id, "审批驳回，退回草稿", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditInbound(Long id) {
        ErpPurchaseInboundDO inbound = getInbound(id);
        OrderStateChecker.require(inbound.getStatus(), inbound.getNo(),
                BizOrderStatus.DRAFT, BizOrderStatus.APPROVING);
        List<ErpPurchaseInboundItemDO> items = inboundItemMapper.selectList(
                new LambdaQueryWrapper<ErpPurchaseInboundItemDO>()
                        .eq(ErpPurchaseInboundItemDO::getInboundId, id));

        // 1. 库存过账（唯一收口：StockPostingApi）
        List<MoveCommand> commands = new ArrayList<>();
        for (ErpPurchaseInboundItemDO item : items) {
            commands.add(new MoveCommand(MOVE_BIZ_TYPE, inbound.getId(), inbound.getNo(), 1,
                    inbound.getWarehouseId(), item.getProductId(), item.getQty(), item.getPrice(),
                    inbound.getSupplierId(), null));
        }
        stockPostingApi.submit(commands);

        // 2. 回写订单执行进度（仅带 orderItemId 的行）
        for (ErpPurchaseInboundItemDO item : items) {
            if (item.getOrderItemId() == null) {
                continue;
            }
            ErpPurchaseOrderItemDO orderItem = orderItemMapper.selectById(item.getOrderItemId());
            if (orderItem == null) {
                throw of(6_01, 2, "订单明细不存在: " + item.getOrderItemId());
            }
            orderItem.setInboundQty(orderItem.getInboundQty().add(item.getQty()));
            orderItemMapper.updateById(orderItem);
        }
        // 3. 订单行全部入库完成则关闭订单
        if (inbound.getOrderId() != null) {
            closeOrderIfFinished(inbound.getOrderId());
        }

        inbound.setStatus(BizOrderStatus.AUDITED.getValue());
        inbound.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        inbound.setAuditTime(LocalDateTime.now());
        inboundMapper.updateById(inbound);
        chatterService.trace(BIZ_TYPE, id, "审核采购入库单 " + inbound.getNo() + "，库存已入账", null);

        // 事务提交后触发财务联动：生成应付 + 记账凭证（失败不回滚业务）
        eventPublisher.publishEvent(new FinanceEvent("purchase_in", "audit", BIZ_TYPE,
                inbound.getId(), inbound.getNo(), inbound.getSupplierId(),
                inbound.getTotalAmount(), null, inbound.getInboundDate()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidInbound(Long id) {
        ErpPurchaseInboundDO inbound = getInbound(id);
        OrderStateChecker.require(inbound.getStatus(), inbound.getNo(), BizOrderStatus.DRAFT);
        inbound.setStatus(BizOrderStatus.VOIDED.getValue());
        inboundMapper.updateById(inbound);
        chatterService.trace(BIZ_TYPE, id, "作废采购入库单 " + inbound.getNo(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseInbound(Long id) {
        ErpPurchaseInboundDO inbound = getInbound(id);
        OrderStateChecker.require(inbound.getStatus(), inbound.getNo(), BizOrderStatus.AUDITED);
        List<ErpPurchaseInboundItemDO> items = inboundItemMapper.selectList(
                new LambdaQueryWrapper<ErpPurchaseInboundItemDO>()
                        .eq(ErpPurchaseInboundItemDO::getInboundId, id));

        // 1. 应付必须未核销 → 作废应付
        finApi.voidPayableByBiz("purchase_in", id);

        // 2. 库存反向出账（材料按当前加权平均成本出库）
        List<MoveCommand> commands = new ArrayList<>();
        for (ErpPurchaseInboundItemDO item : items) {
            commands.add(new MoveCommand("purchase_in_reverse", id, inbound.getNo(), -1,
                    inbound.getWarehouseId(), item.getProductId(), item.getQty(), null,
                    inbound.getSupplierId(), null));
        }
        stockPostingApi.submit(commands);

        // 3. 红字凭证（借贷互换）
        String redVoucherNo = finApi.reverseVoucher("purchase_in", id);

        // 4. 回退订单执行进度
        for (ErpPurchaseInboundItemDO item : items) {
            if (item.getOrderItemId() == null) {
                continue;
            }
            ErpPurchaseOrderItemDO orderItem = orderItemMapper.selectById(item.getOrderItemId());
            if (orderItem != null) {
                BigDecimal newInbound = orderItem.getInboundQty().subtract(item.getQty()).max(BigDecimal.ZERO);
                orderItemMapper.update(null, new LambdaUpdateWrapper<ErpPurchaseOrderItemDO>()
                        .eq(ErpPurchaseOrderItemDO::getId, orderItem.getId())
                        .set(ErpPurchaseOrderItemDO::getInboundQty, newInbound));
            }
        }
        if (inbound.getOrderId() != null) {
            reopenOrderIfNotFinished(inbound.getOrderId());
        }

        inbound.setStatus(STATUS_REVERSED);
        inboundMapper.updateById(inbound);
        chatterService.trace(BIZ_TYPE, id,
                String.format("红字冲销入库单 %s，库存已反向出账，应付已作废，红字凭证 %s",
                        inbound.getNo(), redVoucherNo), null);
    }

    /** 冲销后若订单不再满足全部到货，把已完成状态回退为已审核 */
    private void reopenOrderIfNotFinished(Long orderId) {
        ErpPurchaseOrderDO order = orderMapper.selectById(orderId);
        if (order == null || !BizOrderStatus.DONE.getValue().equals(order.getStatus())) {
            return;
        }
        List<ErpPurchaseOrderItemDO> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<ErpPurchaseOrderItemDO>()
                        .eq(ErpPurchaseOrderItemDO::getOrderId, orderId));
        boolean allFinished = !orderItems.isEmpty() && orderItems.stream()
                .allMatch(i -> i.getInboundQty().compareTo(i.getQty()) >= 0);
        if (!allFinished) {
            order.setStatus(BizOrderStatus.AUDITED.getValue());
            orderMapper.updateById(order);
            chatterService.trace(cn.tiangong.module.pur.service.order.PurchaseOrderServiceImpl.BIZ_TYPE,
                    orderId, "因入库冲销，订单回退为已审核", null);
        }
    }

    private void closeOrderIfFinished(Long orderId) {
        ErpPurchaseOrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        List<ErpPurchaseOrderItemDO> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<ErpPurchaseOrderItemDO>()
                        .eq(ErpPurchaseOrderItemDO::getOrderId, orderId));
        boolean allFinished = !orderItems.isEmpty() && orderItems.stream()
                .allMatch(i -> i.getInboundQty().compareTo(i.getQty()) >= 0);
        if (allFinished && BizOrderStatus.AUDITED.getValue().equals(order.getStatus())) {
            order.setStatus(BizOrderStatus.DONE.getValue());
            orderMapper.updateById(order);
            chatterService.trace(PurchaseOrderServiceImpl.BIZ_TYPE, orderId,
                    "订单全部到货，自动完成", null);
        }
    }

    private void validate(Long supplierId, Long warehouseId, List<Item> items) {
        if (supplierId == null || warehouseId == null) {
            throw of(6_01, 3, "供应商与仓库不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw of(6_01, 4, "入库明细不能为空");
        }
        for (Item item : items) {
            if (item.productId() == null || item.qty() == null || item.qty().signum() <= 0) {
                throw of(6_01, 5, "明细中存在产品/数量不合法的行");
            }
        }
    }

    private ErpPurchaseInboundDO getInbound(Long id) {
        ErpPurchaseInboundDO inbound = inboundMapper.selectById(id);
        if (inbound == null) {
            throw of(6_01, 6, "采购入库单不存在");
        }
        return inbound;
    }
}
