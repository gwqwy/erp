package cn.tiangong.module.mrp.service.order;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpWorkOrderDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpWorkOrderItemDO;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpMaterialReqMapper;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpWorkOrderItemMapper;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpWorkOrderMapper;
import cn.tiangong.module.mrp.service.bom.BomService;
import cn.tiangong.module.stk.api.StockPostingApi;
import cn.tiangong.module.stk.api.StockQueryApi;
import cn.tiangong.module.stk.api.dto.MoveCommand;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    public static final String BIZ_TYPE = "mrp_work_order";
    public static final int STATUS_IN_PRODUCTION = 40;
    public static final int STATUS_DONE = 50;
    public static final int STATUS_VOIDED = 60;

    private final ErpMrpWorkOrderMapper workOrderMapper;
    private final ErpMrpWorkOrderItemMapper workOrderItemMapper;
    private final ErpMrpMaterialReqMapper materialReqMapper;
    private final BomService bomService;
    private final SerialNoApi serialNoApi;
    private final StockPostingApi stockPostingApi;
    private final StockQueryApi stockQueryApi;
    private final ChatterService chatterService;

    @Override
    public PageResult<ErpMrpWorkOrderDO> getWorkOrderPage(PageParam pageParam, String no, Long productId, Integer status) {
        LambdaQueryWrapper<ErpMrpWorkOrderDO> wrapper = new LambdaQueryWrapper<ErpMrpWorkOrderDO>()
                .like(no != null && !no.isBlank(), ErpMrpWorkOrderDO::getNo, no)
                .eq(productId != null, ErpMrpWorkOrderDO::getProductId, productId)
                .eq(status != null, ErpMrpWorkOrderDO::getStatus, status)
                .orderByDesc(ErpMrpWorkOrderDO::getId);
        return workOrderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public WorkOrderDetail getWorkOrderDetail(Long id) {
        ErpMrpWorkOrderDO order = getWorkOrder(id);
        return new WorkOrderDetail(order, getItemList(id));
    }

    @Override
    public List<ErpMrpWorkOrderItemDO> getItemList(Long workOrderId) {
        return workOrderItemMapper.selectList(new LambdaQueryWrapper<ErpMrpWorkOrderItemDO>()
                .eq(ErpMrpWorkOrderItemDO::getWorkOrderId, workOrderId)
                .orderByAsc(ErpMrpWorkOrderItemDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(Long productId, Long bomId, Long warehouseId, BigDecimal qty,
                                LocalDate planStartDate, LocalDate planFinishDate, String remark) {
        validate(productId, bomId, warehouseId, qty);
        ErpMrpWorkOrderDO order = new ErpMrpWorkOrderDO();
        order.setNo(serialNoApi.nextNo("work_order"));
        order.setProductId(productId);
        order.setBomId(bomId);
        order.setWarehouseId(warehouseId);
        order.setQty(qty);
        order.setFinishedQty(BigDecimal.ZERO);
        order.setPlanStartDate(planStartDate != null ? planStartDate : LocalDate.now());
        order.setPlanFinishDate(planFinishDate);
        order.setRemark(remark);
        order.setStatus(BizOrderStatus.DRAFT.getValue());
        workOrderMapper.insert(order);
        chatterService.trace(BIZ_TYPE, order.getId(), "创建生产工单 " + order.getNo(), null);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id) {
        ErpMrpWorkOrderDO order = getWorkOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        workOrderMapper.deleteById(id);
        workOrderItemMapper.delete(new LambdaQueryWrapper<ErpMrpWorkOrderItemDO>()
                .eq(ErpMrpWorkOrderItemDO::getWorkOrderId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditWorkOrder(Long id) {
        ErpMrpWorkOrderDO order = getWorkOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        // 按 BOM 展开到叶子，生成材料需求行
        List<cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomItemDO> leaves =
                bomService.expandToLeaves(order.getProductId(), order.getQty());
        for (cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomItemDO leaf : leaves) {
            ErpMrpWorkOrderItemDO item = new ErpMrpWorkOrderItemDO();
            item.setWorkOrderId(id);
            item.setProductId(leaf.getProductId());
            item.setUnitId(null);
            item.setQty(leaf.getQty());
            item.setIssuedQty(BigDecimal.ZERO);
            workOrderItemMapper.insert(item);
        }
        order.setStatus(BizOrderStatus.AUDITED.getValue());
        order.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        order.setAuditTime(LocalDateTime.now());
        workOrderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id,
                String.format("下达生产工单 %s，展开材料需求 %d 行", order.getNo(), leaves.size()), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidWorkOrder(Long id) {
        ErpMrpWorkOrderDO order = getWorkOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(), BizOrderStatus.DRAFT);
        order.setStatus(STATUS_VOIDED);
        workOrderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "作废生产工单 " + order.getNo(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal finishWorkOrder(Long id, BigDecimal qty, Long warehouseId) {
        ErpMrpWorkOrderDO order = getWorkOrder(id);
        OrderStateChecker.require(order.getStatus(), order.getNo(),
                BizOrderStatus.AUDITED, BizOrderStatus.of(STATUS_IN_PRODUCTION));
        if (qty == null || qty.signum() <= 0) {
            throw of(9_01, 1, "完工数量必须为正数");
        }
        if (order.getFinishedQty().add(qty).compareTo(order.getQty()) > 0) {
            throw of(9_01, 2, String.format("累计完工将超过计划数量（计划 %s，已完工 %s）",
                    order.getQty().stripTrailingZeros().toPlainString(),
                    order.getFinishedQty().stripTrailingZeros().toPlainString()));
        }

        // 成本估算：累计领料成本 ÷ 完工后总数量
        List<Long> reqIds = materialReqMapper.selectList(new LambdaQueryWrapper<cn.tiangong.module.mrp.dal.dataobject.ErpMrpMaterialReqDO>()
                        .eq(cn.tiangong.module.mrp.dal.dataobject.ErpMrpMaterialReqDO::getWorkOrderId, id))
                .stream().map(cn.tiangong.module.mrp.dal.dataobject.ErpMrpMaterialReqDO::getId).toList();
        BigDecimal materialCost = stockQueryApi.sumMoveAmount("material_out", reqIds);
        BigDecimal totalFinished = order.getFinishedQty().add(qty);
        BigDecimal unitCost = materialCost.signum() <= 0 ? BigDecimal.ZERO
                : materialCost.divide(totalFinished, 6, RoundingMode.HALF_UP);

        // 成品完工入库过账
        stockPostingApi.submit(List.of(new MoveCommand("produce_in", order.getId(), order.getNo(), 1,
                warehouseId, order.getProductId(), qty, unitCost, null, null)));

        BigDecimal newFinished = order.getFinishedQty().add(qty);
        LambdaUpdateWrapper<ErpMrpWorkOrderDO> update = new LambdaUpdateWrapper<ErpMrpWorkOrderDO>()
                .eq(ErpMrpWorkOrderDO::getId, id)
                .set(ErpMrpWorkOrderDO::getFinishedQty, newFinished);
        if (newFinished.compareTo(order.getQty()) >= 0) {
            update.set(ErpMrpWorkOrderDO::getStatus, STATUS_DONE);
        } else {
            update.set(ErpMrpWorkOrderDO::getStatus, STATUS_IN_PRODUCTION);
        }
        workOrderMapper.update(null, update);

        boolean done = newFinished.compareTo(order.getQty()) >= 0;
        chatterService.trace(BIZ_TYPE, id,
                String.format("完工入库 %s（成本单价 %s），累计 %s/%s%s",
                        qty.stripTrailingZeros().toPlainString(),
                        unitCost.stripTrailingZeros().toPlainString(),
                        newFinished.stripTrailingZeros().toPlainString(),
                        order.getQty().stripTrailingZeros().toPlainString(),
                        done ? "，工单完成" : ""),
                null);
        return unitCost;
    }

    private void validate(Long productId, Long bomId, Long warehouseId, BigDecimal qty) {
        if (productId == null || bomId == null || warehouseId == null) {
            throw of(9_01, 3, "产品/BOM/仓库不能为空");
        }
        if (qty == null || qty.signum() <= 0) {
            throw of(9_01, 4, "计划数量必须为正数");
        }
    }

    private ErpMrpWorkOrderDO getWorkOrder(Long id) {
        ErpMrpWorkOrderDO order = workOrderMapper.selectById(id);
        if (order == null) {
            throw of(9_01, 5, "生产工单不存在");
        }
        return order;
    }
}
