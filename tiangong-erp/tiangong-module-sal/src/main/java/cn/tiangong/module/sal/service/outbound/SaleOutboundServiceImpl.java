package cn.tiangong.module.sal.service.outbound;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.PartnerApi;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.bpm.service.ApprovalService;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOrderDO;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOrderItemDO;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOutboundDO;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOutboundItemDO;
import cn.tiangong.module.sal.dal.mysql.ErpSaleOrderItemMapper;
import cn.tiangong.module.sal.dal.mysql.ErpSaleOrderMapper;
import cn.tiangong.module.sal.dal.mysql.ErpSaleOutboundItemMapper;
import cn.tiangong.module.sal.dal.mysql.ErpSaleOutboundMapper;
import cn.tiangong.module.sal.service.order.SaleOrderServiceImpl;
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
public class SaleOutboundServiceImpl implements SaleOutboundService {

    public static final String BIZ_TYPE = "sal_outbound";
    private static final String MOVE_BIZ_TYPE = "sale_out";

    private final ErpSaleOutboundMapper outboundMapper;
    private final ErpSaleOutboundItemMapper outboundItemMapper;
    private final ErpSaleOrderMapper orderMapper;
    private final ErpSaleOrderItemMapper orderItemMapper;
    private final SerialNoApi serialNoApi;
    private final StockPostingApi stockPostingApi;
    private final ChatterService chatterService;
    private final ApplicationEventPublisher eventPublisher;
    private final FinApi finApi;
    private final ApprovalService approvalService;
    private final PartnerApi partnerApi;

    /** 60 = 已冲销（红字） */
    public static final int STATUS_REVERSED = 60;

    @Override
    public PageResult<ErpSaleOutboundDO> getOutboundPage(PageParam pageParam, String no, Long customerId,
                                                         Long orderId, Integer status) {
        LambdaQueryWrapper<ErpSaleOutboundDO> wrapper = new LambdaQueryWrapper<ErpSaleOutboundDO>()
                .like(no != null && !no.isBlank(), ErpSaleOutboundDO::getNo, no)
                .eq(customerId != null, ErpSaleOutboundDO::getCustomerId, customerId)
                .eq(orderId != null, ErpSaleOutboundDO::getOrderId, orderId)
                .eq(status != null, ErpSaleOutboundDO::getStatus, status)
                .orderByDesc(ErpSaleOutboundDO::getId);
        return outboundMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public OutboundDetail getOutboundDetail(Long id) {
        ErpSaleOutboundDO outbound = getOutbound(id);
        List<ErpSaleOutboundItemDO> items = outboundItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOutboundItemDO>()
                        .eq(ErpSaleOutboundItemDO::getOutboundId, id)
                        .orderByAsc(ErpSaleOutboundItemDO::getId));
        return new OutboundDetail(outbound, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOutbound(Long customerId, Long warehouseId, Long orderId, LocalDate outboundDate,
                               String remark, List<Item> items) {
        validate(customerId, warehouseId, items);
        if (orderId != null) {
            ErpSaleOrderDO order = orderMapper.selectById(orderId);
            if (order == null) {
                throw of(7_01, 1, "来源销售订单不存在");
            }
            OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.AUDITED);
        }
        ErpSaleOutboundDO outbound = new ErpSaleOutboundDO();
        outbound.setNo(serialNoApi.nextNo("sale_outbound"));
        outbound.setCustomerId(customerId);
        outbound.setWarehouseId(warehouseId);
        outbound.setOrderId(orderId);
        outbound.setOutboundDate(outboundDate != null ? outboundDate : LocalDate.now());
        outbound.setRemark(remark);
        outbound.setTotalQty(items.stream().map(Item::qty).reduce(BigDecimal.ZERO, BigDecimal::add));
        outbound.setTotalAmount(items.stream()
                .map(i -> i.qty().multiply(i.price())).reduce(BigDecimal.ZERO, BigDecimal::add));
        outbound.setStatus(BizOrderStatus.DRAFT.getValue());
        outboundMapper.insert(outbound);
        for (Item item : items) {
            ErpSaleOutboundItemDO itemDO = new ErpSaleOutboundItemDO();
            itemDO.setOutboundId(outbound.getId());
            itemDO.setOrderItemId(item.orderItemId());
            itemDO.setProductId(item.productId());
            itemDO.setUnitId(item.unitId());
            itemDO.setQty(item.qty());
            itemDO.setPrice(item.price());
            itemDO.setAmount(item.qty().multiply(item.price()));
            itemDO.setRemark(item.remark());
            outboundItemMapper.insert(itemDO);
        }
        chatterService.trace(BIZ_TYPE, outbound.getId(), "创建销售出库单 " + outbound.getNo(), null);
        return outbound.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        ErpSaleOutboundDO outbound = getOutbound(id);
        OrderStateChecker.require(outbound.getStatus(), outbound.getNo(), BizOrderStatus.DRAFT);
        if (!approvalService.submit("sal_outbound", BIZ_TYPE, id, outbound.getNo())) {
            throw of(7_01, 7, "销售出库未配置审批流，请直接审核");
        }
        outbound.setStatus(BizOrderStatus.APPROVING.getValue());
        outboundMapper.updateById(outbound);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalRejected(Long id) {
        ErpSaleOutboundDO outbound = getOutbound(id);
        OrderStateChecker.require(outbound.getStatus(), outbound.getNo(), BizOrderStatus.APPROVING);
        outbound.setStatus(BizOrderStatus.DRAFT.getValue());
        outboundMapper.updateById(outbound);
        chatterService.trace(BIZ_TYPE, id, "审批驳回，退回草稿", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOutbound(Long id) {
        ErpSaleOutboundDO outbound = getOutbound(id);
        OrderStateChecker.require(outbound.getStatus(), outbound.getNo(), BizOrderStatus.DRAFT);
        outboundMapper.deleteById(id);
        outboundItemMapper.delete(new LambdaQueryWrapper<ErpSaleOutboundItemDO>()
                .eq(ErpSaleOutboundItemDO::getOutboundId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOutbound(Long id) {
        ErpSaleOutboundDO outbound = getOutbound(id);
        OrderStateChecker.require(outbound.getStatus(), outbound.getNo(),
                BizOrderStatus.DRAFT, BizOrderStatus.APPROVING);
        // 信用额度控制：客户未核销应收 + 本单金额 不得超过信用额度（未设额度不限制）
        java.math.BigDecimal creditLimit = partnerCreditLimit(outbound.getCustomerId());
        if (creditLimit != null) {
            java.math.BigDecimal unsettled = finApi.unsettledReceivableOfPartner(outbound.getCustomerId());
            if (unsettled.add(outbound.getTotalAmount()).compareTo(creditLimit) > 0) {
                throw of(7_01, 7, String.format("客户信用额度不足：额度 %s，已占用未核销应收 %s，本单 %s",
                        creditLimit.stripTrailingZeros().toPlainString(),
                        unsettled.stripTrailingZeros().toPlainString(),
                        outbound.getTotalAmount().stripTrailingZeros().toPlainString()));
            }
        }
        List<ErpSaleOutboundItemDO> items = outboundItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOutboundItemDO>()
                        .eq(ErpSaleOutboundItemDO::getOutboundId, id));

        // 1. 库存过账（出库方向，库存不足时整体回滚并提示）
        List<MoveCommand> commands = new ArrayList<>();
        for (ErpSaleOutboundItemDO item : items) {
            commands.add(new MoveCommand(MOVE_BIZ_TYPE, outbound.getId(), outbound.getNo(), -1,
                    outbound.getWarehouseId(), item.getProductId(), item.getQty(), null,
                    outbound.getCustomerId(), null));
        }
        List<BigDecimal> costs = stockPostingApi.submit(commands);
        BigDecimal costAmount = costs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. 回写订单执行进度
        for (ErpSaleOutboundItemDO item : items) {
            if (item.getOrderItemId() == null) {
                continue;
            }
            ErpSaleOrderItemDO orderItem = orderItemMapper.selectById(item.getOrderItemId());
            if (orderItem == null) {
                throw of(7_01, 2, "订单明细不存在: " + item.getOrderItemId());
            }
            orderItem.setOutboundQty(orderItem.getOutboundQty().add(item.getQty()));
            orderItemMapper.updateById(orderItem);
        }
        // 3. 订单行全部出库完成则关闭
        if (outbound.getOrderId() != null) {
            closeOrderIfFinished(outbound.getOrderId());
        }

        outbound.setStatus(BizOrderStatus.AUDITED.getValue());
        outbound.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        outbound.setAuditTime(LocalDateTime.now());
        outboundMapper.updateById(outbound);
        chatterService.trace(BIZ_TYPE, id, "审核销售出库单 " + outbound.getNo() + "，库存已出账", null);

        // 事务提交后触发财务联动：生成应收 + 收入凭证 + 成本结转凭证（失败不回滚业务）
        eventPublisher.publishEvent(new FinanceEvent("sale_out", "audit", BIZ_TYPE,
                outbound.getId(), outbound.getNo(), outbound.getCustomerId(),
                outbound.getTotalAmount(), costAmount, outbound.getOutboundDate()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseOutbound(Long id) {
        ErpSaleOutboundDO outbound = getOutbound(id);
        OrderStateChecker.require(outbound.getStatus(), outbound.getNo(), BizOrderStatus.AUDITED);
        List<ErpSaleOutboundItemDO> items = outboundItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOutboundItemDO>()
                        .eq(ErpSaleOutboundItemDO::getOutboundId, id));

        // 1. 应收必须未核销 → 作废应收
        finApi.voidReceivableByBiz("sale_out", id);

        // 2. 库存反向入库（货物按当前加权平均成本回库）
        List<MoveCommand> commands = new ArrayList<>();
        for (ErpSaleOutboundItemDO item : items) {
            commands.add(new MoveCommand("sale_out_reverse", id, outbound.getNo(), 1,
                    outbound.getWarehouseId(), item.getProductId(), item.getQty(),
                    item.getPrice(), outbound.getCustomerId(), null));
        }
        stockPostingApi.submit(commands);

        // 3. 红字凭证（借贷互换：冲收入与应收）
        String redVoucherNo = finApi.reverseVoucher("sale_out", id);

        // 4. 回退订单执行进度
        for (ErpSaleOutboundItemDO item : items) {
            if (item.getOrderItemId() == null) {
                continue;
            }
            ErpSaleOrderItemDO orderItem = orderItemMapper.selectById(item.getOrderItemId());
            if (orderItem != null) {
                orderItemMapper.update(null, new LambdaUpdateWrapper<ErpSaleOrderItemDO>()
                        .eq(ErpSaleOrderItemDO::getId, orderItem.getId())
                        .set(ErpSaleOrderItemDO::getOutboundQty,
                                orderItem.getOutboundQty().subtract(item.getQty()).max(BigDecimal.ZERO)));
            }
        }
        if (outbound.getOrderId() != null) {
            ErpSaleOrderDO order = orderMapper.selectById(outbound.getOrderId());
            if (order != null && BizOrderStatus.DONE.getValue().equals(order.getStatus())) {
                List<ErpSaleOrderItemDO> orderItems = orderItemMapper.selectList(
                        new LambdaQueryWrapper<ErpSaleOrderItemDO>()
                                .eq(ErpSaleOrderItemDO::getOrderId, outbound.getOrderId()));
                boolean allFinished = !orderItems.isEmpty() && orderItems.stream()
                        .allMatch(i -> i.getOutboundQty().compareTo(i.getQty()) >= 0);
                if (!allFinished) {
                    order.setStatus(BizOrderStatus.AUDITED.getValue());
                    orderMapper.updateById(order);
                    chatterService.trace(cn.tiangong.module.sal.service.order.SaleOrderServiceImpl.BIZ_TYPE,
                            order.getId(), "因出库冲销，订单回退为已审核", null);
                }
            }
        }

        outbound.setStatus(STATUS_REVERSED);
        outboundMapper.updateById(outbound);
        chatterService.trace(BIZ_TYPE, id,
                String.format("红字冲销出库单 %s，货物已回库，应收已作废，红字凭证 %s",
                        outbound.getNo(), redVoucherNo), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidOutbound(Long id) {
        ErpSaleOutboundDO outbound = getOutbound(id);
        OrderStateChecker.require(outbound.getStatus(), outbound.getNo(), BizOrderStatus.DRAFT);
        outbound.setStatus(BizOrderStatus.VOIDED.getValue());
        outboundMapper.updateById(outbound);
        chatterService.trace(BIZ_TYPE, id, "作废销售出库单 " + outbound.getNo(), null);
    }

    /** 客户信用额度（未设置返回 null = 不限制） */
    private BigDecimal partnerCreditLimit(Long customerId) {
        return partnerApi.getCreditLimit(customerId);
    }

    private void closeOrderIfFinished(Long orderId) {
        ErpSaleOrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        List<ErpSaleOrderItemDO> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOrderItemDO>()
                        .eq(ErpSaleOrderItemDO::getOrderId, orderId));
        boolean allFinished = !orderItems.isEmpty() && orderItems.stream()
                .allMatch(i -> i.getOutboundQty().compareTo(i.getQty()) >= 0);
        if (allFinished && BizOrderStatus.AUDITED.getValue().equals(order.getStatus())) {
            order.setStatus(BizOrderStatus.DONE.getValue());
            orderMapper.updateById(order);
            chatterService.trace(SaleOrderServiceImpl.BIZ_TYPE, orderId, "订单全部发货，自动完成", null);
        }
    }

    private void validate(Long customerId, Long warehouseId, List<Item> items) {
        if (customerId == null || warehouseId == null) {
            throw of(7_01, 3, "客户与仓库不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw of(7_01, 4, "出库明细不能为空");
        }
        for (Item item : items) {
            if (item.productId() == null || item.qty() == null || item.qty().signum() <= 0) {
                throw of(7_01, 5, "明细中存在产品/数量不合法的行");
            }
        }
    }

    private ErpSaleOutboundDO getOutbound(Long id) {
        ErpSaleOutboundDO outbound = outboundMapper.selectById(id);
        if (outbound == null) {
            throw of(7_01, 6, "销售出库单不存在");
        }
        return outbound;
    }
}
