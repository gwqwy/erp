package cn.tiangong.module.stk.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.ProductApi;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.fin.api.dto.FinanceEvent;
import cn.tiangong.module.stk.api.StockPostingApi;
import cn.tiangong.module.stk.api.dto.MoveCommand;
import cn.tiangong.module.stk.dal.dataobject.StockCheckDO;
import cn.tiangong.module.stk.dal.dataobject.StockCheckItemDO;
import cn.tiangong.module.stk.dal.dataobject.StockOtherDO;
import cn.tiangong.module.stk.dal.dataobject.StockOtherItemDO;
import cn.tiangong.module.stk.dal.dataobject.StockQuantDO;
import cn.tiangong.module.stk.dal.dataobject.StockTransferDO;
import cn.tiangong.module.stk.dal.dataobject.StockTransferItemDO;
import cn.tiangong.module.stk.dal.mysql.StockCheckItemMapper;
import cn.tiangong.module.stk.dal.mysql.StockCheckMapper;
import cn.tiangong.module.stk.dal.mysql.StockOtherItemMapper;
import cn.tiangong.module.stk.dal.mysql.StockOtherMapper;
import cn.tiangong.module.stk.dal.mysql.StockQuantMapper;
import cn.tiangong.module.stk.dal.mysql.StockTransferItemMapper;
import cn.tiangong.module.stk.dal.mysql.StockTransferMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 库存扩展业务：调拨 / 盘点（盈亏自动过账+凭证）/ 其他出入库。
 */
@RequiredArgsConstructor
@Service
public class StockBizExtService {

    public static final String BIZ_TRANSFER = "stock_transfer";
    public static final String BIZ_CHECK = "stock_check";
    public static final String BIZ_OTHER = "stock_other";

    private final StockTransferMapper transferMapper;
    private final StockTransferItemMapper transferItemMapper;
    private final StockCheckMapper checkMapper;
    private final StockCheckItemMapper checkItemMapper;
    private final StockOtherMapper otherMapper;
    private final StockOtherItemMapper otherItemMapper;
    private final StockQuantMapper quantMapper;
    private final StockPostingApi stockPostingApi;
    private final SerialNoApi serialNoApi;
    private final ProductApi productApi;
    private final ChatterService chatterService;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 调拨 ====================

    public PageResult<StockTransferDO> getTransferPage(PageParam pageParam, String no, Integer status) {
        return transferMapper.selectPage(pageParam, new LambdaQueryWrapper<StockTransferDO>()
                .like(no != null && !no.isBlank(), StockTransferDO::getNo, no)
                .eq(status != null, StockTransferDO::getStatus, status)
                .orderByDesc(StockTransferDO::getId));
    }

    public TransferDetail getTransferDetail(Long id) {
        StockTransferDO transfer = transferMapper.selectById(id);
        if (transfer == null) {
            throw of(3_10, 1, "调拨单不存在");
        }
        return new TransferDetail(transfer, transferItemMapper.selectList(
                new LambdaQueryWrapper<StockTransferItemDO>()
                        .eq(StockTransferItemDO::getTransferId, id)
                        .orderByAsc(StockTransferItemDO::getId)));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createTransferEx(Long fromWarehouseId, Long toWarehouseId, LocalDate transferDate,
                                 String remark, List<TransferItem> items) {
        if (fromWarehouseId == null || toWarehouseId == null || fromWarehouseId.equals(toWarehouseId)) {
            throw of(3_10, 2, "调出/调入仓库不能相同或为空");
        }
        if (items == null || items.isEmpty()) {
            throw of(3_10, 3, "调拨明细不能为空");
        }
        StockTransferDO transfer = new StockTransferDO();
        transfer.setNo(serialNoApi.nextNo("stock_transfer"));
        transfer.setFromWarehouseId(fromWarehouseId);
        transfer.setToWarehouseId(toWarehouseId);
        transfer.setTransferDate(transferDate != null ? transferDate : LocalDate.now());
        transfer.setTotalQty(items.stream().map(TransferItem::qty).reduce(BigDecimal.ZERO, BigDecimal::add));
        transfer.setRemark(remark);
        transfer.setStatus(BizOrderStatus.DRAFT.getValue());
        transferMapper.insert(transfer);
        for (TransferItem item : items) {
            StockTransferItemDO itemDO = new StockTransferItemDO();
            itemDO.setTransferId(transfer.getId());
            itemDO.setProductId(item.productId());
            itemDO.setQty(item.qty());
            itemDO.setRemark(item.remark());
            transferItemMapper.insert(itemDO);
        }
        chatterService.trace(BIZ_TRANSFER, transfer.getId(), "创建调拨单 " + transfer.getNo(), null);
        return transfer.getId();
    }

    /** 审核：from 仓出（FIFO 拿成本）→ to 仓入（同成本），两笔流水 */
    @Transactional(rollbackFor = Exception.class)
    public void auditTransfer(Long id) {
        StockTransferDO transfer = transferMapper.selectById(id);
        if (transfer == null) {
            throw of(3_10, 1, "调拨单不存在");
        }
        OrderStateChecker.require(transfer.getStatus(), transfer.getNo(), BizOrderStatus.DRAFT);
        List<StockTransferItemDO> items = transferItemMapper.selectList(
                new LambdaQueryWrapper<StockTransferItemDO>()
                        .eq(StockTransferItemDO::getTransferId, id));
        BigDecimal totalCost = BigDecimal.ZERO;
        for (StockTransferItemDO item : items) {
            BigDecimal cost = stockPostingApi.submit(List.of(new MoveCommand(BIZ_TRANSFER, id, transfer.getNo(), -1,
                    transfer.getFromWarehouseId(), item.getProductId(), item.getQty(), null, null, null)))
                    .stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unitCost = item.getQty().signum() == 0 ? BigDecimal.ZERO
                    : cost.divide(item.getQty(), 6, java.math.RoundingMode.HALF_UP);
            stockPostingApi.submit(List.of(new MoveCommand(BIZ_TRANSFER, id, transfer.getNo(), 1,
                    transfer.getToWarehouseId(), item.getProductId(), item.getQty(), unitCost, null, null)));
            totalCost = totalCost.add(cost);
        }
        transfer.setStatus(BizOrderStatus.AUDITED.getValue());
        transfer.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        transfer.setAuditTime(LocalDateTime.now());
        transferMapper.updateById(transfer);
        chatterService.trace(BIZ_TRANSFER, id, String.format("审核调拨单 %s，货权转移完成（成本 %s）",
                transfer.getNo(), totalCost.stripTrailingZeros().toPlainString()), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidTransfer(Long id) {
        StockTransferDO transfer = transferMapper.selectById(id);
        if (transfer == null) {
            throw of(3_10, 1, "调拨单不存在");
        }
        OrderStateChecker.require(transfer.getStatus(), transfer.getNo(), BizOrderStatus.DRAFT);
        transfer.setStatus(BizOrderStatus.VOIDED.getValue());
        transferMapper.updateById(transfer);
    }

    public void deleteTransfer(Long id) {
        StockTransferDO transfer = transferMapper.selectById(id);
        if (transfer == null) {
            throw of(3_10, 1, "调拨单不存在");
        }
        OrderStateChecker.require(transfer.getStatus(), transfer.getNo(), BizOrderStatus.DRAFT);
        transferMapper.deleteById(id);
        transferItemMapper.delete(new LambdaQueryWrapper<StockTransferItemDO>()
                .eq(StockTransferItemDO::getTransferId, id));
    }

    // ==================== 盘点 ====================

    public PageResult<StockCheckDO> getCheckPage(PageParam pageParam, String no, Integer status) {
        return checkMapper.selectPage(pageParam, new LambdaQueryWrapper<StockCheckDO>()
                .like(no != null && !no.isBlank(), StockCheckDO::getNo, no)
                .eq(status != null, StockCheckDO::getStatus, status)
                .orderByDesc(StockCheckDO::getId));
    }

    public CheckDetail getCheckDetail(Long id) {
        StockCheckDO check = checkMapper.selectById(id);
        if (check == null) {
            throw of(3_11, 1, "盘点单不存在");
        }
        return new CheckDetail(check, checkItemMapper.selectList(
                new LambdaQueryWrapper<StockCheckItemDO>()
                        .eq(StockCheckItemDO::getCheckId, id)
                        .orderByAsc(StockCheckItemDO::getId)));
    }

    /** 创建盘点单：按仓库当前 quant 生成账面快照明细 */
    @Transactional(rollbackFor = Exception.class)
    public Long createCheck(Long warehouseId, String remark) {
        if (warehouseId == null) {
            throw of(3_11, 2, "仓库不能为空");
        }
        List<StockQuantDO> quants = quantMapper.selectList(new LambdaQueryWrapper<StockQuantDO>()
                .eq(StockQuantDO::getWarehouseId, warehouseId)
                .eq(StockQuantDO::getBatchNo, ""));
        StockCheckDO check = new StockCheckDO();
        check.setNo(serialNoApi.nextNo("stock_check"));
        check.setWarehouseId(warehouseId);
        check.setCheckDate(LocalDate.now());
        check.setRemark(remark);
        check.setStatus(BizOrderStatus.DRAFT.getValue());
        checkMapper.insert(check);
        for (StockQuantDO quant : quants) {
            StockCheckItemDO item = new StockCheckItemDO();
            item.setCheckId(check.getId());
            item.setProductId(quant.getProductId());
            item.setBookQty(quant.getQty());
            item.setRealQty(quant.getQty()); // 默认实盘=账面，用户修改
            checkItemMapper.insert(item);
        }
        chatterService.trace(BIZ_CHECK, check.getId(),
                String.format("创建盘点单 %s（%d 行账面快照）", check.getNo(), quants.size()), null);
        return check.getId();
    }

    /** 更新实盘数量 */
    public void updateRealQty(Long checkItemId, BigDecimal realQty) {
        StockCheckItemDO item = checkItemMapper.selectById(checkItemId);
        if (item == null) {
            throw of(3_11, 3, "盘点明细不存在");
        }
        if (realQty == null || realQty.signum() < 0) {
            throw of(3_11, 4, "实盘数量不合法");
        }
        checkItemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<StockCheckItemDO>()
                .eq(StockCheckItemDO::getId, checkItemId)
                .set(StockCheckItemDO::getRealQty, realQty));
    }

    /** 审核：盈亏自动过账（盘盈入库/盘亏出库）+ 凭证事件 */
    @Transactional(rollbackFor = Exception.class)
    public void auditCheck(Long id) {
        StockCheckDO check = checkMapper.selectById(id);
        if (check == null) {
            throw of(3_11, 1, "盘点单不存在");
        }
        OrderStateChecker.require(check.getStatus(), check.getNo(), BizOrderStatus.DRAFT);
        List<StockCheckItemDO> items = checkItemMapper.selectList(
                new LambdaQueryWrapper<StockCheckItemDO>()
                        .eq(StockCheckItemDO::getCheckId, id));
        BigDecimal gainTotal = BigDecimal.ZERO;
        BigDecimal lossTotal = BigDecimal.ZERO;
        for (StockCheckItemDO item : items) {
            BigDecimal diff = item.getRealQty().subtract(item.getBookQty());
            if (diff.signum() == 0) {
                continue;
            }
            BigDecimal avgCost = avgCostOf(check.getWarehouseId(), item.getProductId());
            BigDecimal amount = diff.abs().multiply(avgCost);
            stockPostingApi.submit(List.of(new MoveCommand(diff.signum() > 0 ? "check_in" : "check_out",
                    id, check.getNo(), diff.signum() > 0 ? 1 : -1, check.getWarehouseId(),
                    item.getProductId(), diff.abs(), avgCost, null, null)));
            if (diff.signum() > 0) {
                gainTotal = gainTotal.add(amount);
            } else {
                lossTotal = lossTotal.add(amount);
            }
        }
        check.setStatus(BizOrderStatus.AUDITED.getValue());
        check.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        check.setAuditTime(LocalDateTime.now());
        checkMapper.updateById(check);
        chatterService.trace(BIZ_CHECK, id, String.format("审核盘点单 %s：盘盈 %s，盘亏 %s（金额）",
                check.getNo(), gainTotal.stripTrailingZeros().toPlainString(),
                lossTotal.stripTrailingZeros().toPlainString()), null);
        if (gainTotal.signum() > 0) {
            eventPublisher.publishEvent(new FinanceEvent("stock_check_gain", "audit", BIZ_CHECK,
                    id, check.getNo(), null, gainTotal, null, check.getCheckDate()));
        }
        if (lossTotal.signum() > 0) {
            eventPublisher.publishEvent(new FinanceEvent("stock_check_loss", "audit", BIZ_CHECK,
                    id, check.getNo(), null, lossTotal, null, check.getCheckDate()));
        }
    }

    private BigDecimal avgCostOf(Long warehouseId, Long productId) {
        StockQuantDO quant = quantMapper.selectOne(new LambdaQueryWrapper<StockQuantDO>()
                .eq(StockQuantDO::getWarehouseId, warehouseId)
                .eq(StockQuantDO::getProductId, productId)
                .eq(StockQuantDO::getBatchNo, "").last("LIMIT 1"));
        return quant == null || quant.getAvgCost() == null ? BigDecimal.ZERO : quant.getAvgCost();
    }

    public void deleteCheck(Long id) {
        StockCheckDO check = checkMapper.selectById(id);
        if (check == null) {
            throw of(3_11, 1, "盘点单不存在");
        }
        OrderStateChecker.require(check.getStatus(), check.getNo(), BizOrderStatus.DRAFT);
        checkMapper.deleteById(id);
        checkItemMapper.delete(new LambdaQueryWrapper<StockCheckItemDO>()
                .eq(StockCheckItemDO::getCheckId, id));
    }

    // ==================== 其他出入库 ====================

    public PageResult<StockOtherDO> getOtherPage(PageParam pageParam, String no, String otherType, Integer status) {
        return otherMapper.selectPage(pageParam, new LambdaQueryWrapper<StockOtherDO>()
                .like(no != null && !no.isBlank(), StockOtherDO::getNo, no)
                .eq(otherType != null && !otherType.isBlank(), StockOtherDO::getOtherType, otherType)
                .eq(status != null, StockOtherDO::getStatus, status)
                .orderByDesc(StockOtherDO::getId));
    }

    public OtherDetail getOtherDetail(Long id) {
        StockOtherDO other = otherMapper.selectById(id);
        if (other == null) {
            throw of(3_12, 1, "其他出入库单不存在");
        }
        return new OtherDetail(other, otherItemMapper.selectList(
                new LambdaQueryWrapper<StockOtherItemDO>()
                        .eq(StockOtherItemDO::getOtherId, id)
                        .orderByAsc(StockOtherItemDO::getId)));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createOther(String otherType, Long warehouseId, LocalDate otherDate,
                            String remark, List<OtherItem> items) {
        if (!"other_in".equals(otherType) && !"other_out".equals(otherType)) {
            throw of(3_12, 2, "类型必须为 other_in/other_out");
        }
        if (warehouseId == null || items == null || items.isEmpty()) {
            throw of(3_12, 3, "仓库与明细不能为空");
        }
        StockOtherDO other = new StockOtherDO();
        other.setNo(serialNoApi.nextNo("stock_other_" + ("other_in".equals(otherType) ? "in" : "out")));
        other.setOtherType(otherType);
        other.setWarehouseId(warehouseId);
        other.setOtherDate(otherDate != null ? otherDate : LocalDate.now());
        other.setTotalQty(items.stream().map(OtherItem::qty).reduce(BigDecimal.ZERO, BigDecimal::add));
        other.setRemark(remark);
        other.setStatus(BizOrderStatus.DRAFT.getValue());
        otherMapper.insert(other);
        for (OtherItem item : items) {
            StockOtherItemDO itemDO = new StockOtherItemDO();
            itemDO.setOtherId(other.getId());
            itemDO.setProductId(item.productId());
            itemDO.setQty(item.qty());
            itemDO.setPrice(item.price() == null ? BigDecimal.ZERO : item.price());
            itemDO.setRemark(item.remark());
            otherItemMapper.insert(itemDO);
        }
        chatterService.trace(BIZ_OTHER, other.getId(), "创建其他出入库单 " + other.getNo(), null);
        return other.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditOther(Long id) {
        StockOtherDO other = otherMapper.selectById(id);
        if (other == null) {
            throw of(3_12, 1, "其他出入库单不存在");
        }
        OrderStateChecker.require(other.getStatus(), other.getNo(), BizOrderStatus.DRAFT);
        List<StockOtherItemDO> items = otherItemMapper.selectList(
                new LambdaQueryWrapper<StockOtherItemDO>()
                        .eq(StockOtherItemDO::getOtherId, id));
        List<MoveCommand> commands = new ArrayList<>();
        int dir = "other_in".equals(other.getOtherType()) ? 1 : -1;
        for (StockOtherItemDO item : items) {
            commands.add(new MoveCommand(other.getOtherType(), id, other.getNo(), dir,
                    other.getWarehouseId(), item.getProductId(), item.getQty(),
                    dir == 1 ? item.getPrice() : null, null, null));
        }
        stockPostingApi.submit(commands);
        other.setStatus(BizOrderStatus.AUDITED.getValue());
        other.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        other.setAuditTime(LocalDateTime.now());
        otherMapper.updateById(other);
        chatterService.trace(BIZ_OTHER, id, String.format("审核其他出入库单 %s，库存已过账", other.getNo()), null);
    }

    public void voidOther(Long id) {
        StockOtherDO other = otherMapper.selectById(id);
        if (other == null) {
            throw of(3_12, 1, "其他出入库单不存在");
        }
        OrderStateChecker.require(other.getStatus(), other.getNo(), BizOrderStatus.DRAFT);
        other.setStatus(BizOrderStatus.VOIDED.getValue());
        otherMapper.updateById(other);
    }

    public void deleteOther(Long id) {
        StockOtherDO other = otherMapper.selectById(id);
        if (other == null) {
            throw of(3_12, 1, "其他出入库单不存在");
        }
        OrderStateChecker.require(other.getStatus(), other.getNo(), BizOrderStatus.DRAFT);
        otherMapper.deleteById(id);
        otherItemMapper.delete(new LambdaQueryWrapper<StockOtherItemDO>()
                .eq(StockOtherItemDO::getOtherId, id));
    }

    // ==================== 预警 ====================

    /** 全仓库库存汇总，低于安全下限标"低量"，高于上限标"超储" */
    public List<cn.tiangong.module.stk.controller.admin.StockBizExtController.AlertVO> stockAlertList() {
        List<StockQuantDO> quants = quantMapper.selectList(new LambdaQueryWrapper<StockQuantDO>()
                .gt(StockQuantDO::getQty, BigDecimal.ZERO));
        Map<Long, BigDecimal> totals = new java.util.HashMap<>();
        for (StockQuantDO q : quants) {
            totals.merge(q.getProductId(), q.getQty(), BigDecimal::add);
        }
        List<cn.tiangong.module.stk.controller.admin.StockBizExtController.AlertVO> list = new ArrayList<>();
        for (cn.tiangong.module.bas.api.ProductApi.StockLimit limit : productApi.getStockLimits()) {
            BigDecimal qty = totals.getOrDefault(limit.productId(), BigDecimal.ZERO);
            String level = null;
            if (limit.stockMin() != null && qty.compareTo(limit.stockMin()) < 0) {
                level = "low";
            } else if (limit.stockMax() != null && qty.compareTo(limit.stockMax()) > 0) {
                level = "over";
            }
            if (level != null) {
                list.add(new cn.tiangong.module.stk.controller.admin.StockBizExtController.AlertVO(
                        String.valueOf(limit.productId()), limit.name(), qty, limit.stockMin(), limit.stockMax(), level));
            }
        }
        return list;
    }

    // ==================== 载体 ====================

    public record TransferItem(Long productId, BigDecimal qty, String remark) {
    }

    public record TransferDetail(StockTransferDO transfer, List<StockTransferItemDO> items) {
    }

    public record CheckDetail(StockCheckDO check, List<StockCheckItemDO> items) {
    }

    public record OtherItem(Long productId, BigDecimal qty, BigDecimal price, String remark) {
    }

    public record OtherDetail(StockOtherDO other, List<StockOtherItemDO> items) {
    }
}
