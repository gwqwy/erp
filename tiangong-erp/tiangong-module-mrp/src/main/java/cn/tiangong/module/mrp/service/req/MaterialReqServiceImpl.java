package cn.tiangong.module.mrp.service.req;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpMaterialReqDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpMaterialReqItemDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpWorkOrderDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpWorkOrderItemDO;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpMaterialReqItemMapper;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpMaterialReqMapper;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpWorkOrderItemMapper;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpWorkOrderMapper;
import cn.tiangong.module.mrp.service.order.WorkOrderServiceImpl;
import cn.tiangong.module.stk.api.StockPostingApi;
import cn.tiangong.module.stk.api.dto.MoveCommand;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class MaterialReqServiceImpl implements MaterialReqService {

    public static final String BIZ_TYPE = "mrp_material_req";
    private static final String MOVE_BIZ_TYPE = "material_out";

    private final ErpMrpMaterialReqMapper reqMapper;
    private final ErpMrpMaterialReqItemMapper reqItemMapper;
    private final ErpMrpWorkOrderMapper workOrderMapper;
    private final ErpMrpWorkOrderItemMapper workOrderItemMapper;
    private final SerialNoApi serialNoApi;
    private final StockPostingApi stockPostingApi;
    private final ChatterService chatterService;

    @Override
    public PageResult<ErpMrpMaterialReqDO> getReqPage(PageParam pageParam, String no, Long workOrderId, Integer status) {
        LambdaQueryWrapper<ErpMrpMaterialReqDO> wrapper = new LambdaQueryWrapper<ErpMrpMaterialReqDO>()
                .like(no != null && !no.isBlank(), ErpMrpMaterialReqDO::getNo, no)
                .eq(workOrderId != null, ErpMrpMaterialReqDO::getWorkOrderId, workOrderId)
                .eq(status != null, ErpMrpMaterialReqDO::getStatus, status)
                .orderByDesc(ErpMrpMaterialReqDO::getId);
        return reqMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public ReqDetail getReqDetail(Long id) {
        ErpMrpMaterialReqDO req = getReq(id);
        List<ErpMrpMaterialReqItemDO> items = reqItemMapper.selectList(
                new LambdaQueryWrapper<ErpMrpMaterialReqItemDO>()
                        .eq(ErpMrpMaterialReqItemDO::getReqId, id)
                        .orderByAsc(ErpMrpMaterialReqItemDO::getId));
        return new ReqDetail(req, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReq(Long workOrderId, Long warehouseId, String remark, List<Item> items) {
        validate(workOrderId, warehouseId, items);
        ErpMrpWorkOrderDO order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            throw of(9_02, 1, "生产工单不存在");
        }
        OrderStateChecker.require(order.getStatus(), order.getNo(),
                BizOrderStatus.AUDITED, BizOrderStatus.of(WorkOrderServiceImpl.STATUS_IN_PRODUCTION));
        // 校验领料不超需求
        for (Item item : items) {
            ErpMrpWorkOrderItemDO woItem = workOrderItemMapper.selectById(item.workOrderItemId());
            if (woItem == null || !String.valueOf(woItem.getWorkOrderId()).equals(String.valueOf(workOrderId))) {
                throw of(9_02, 2, "领料行不属于该工单: " + item.workOrderItemId());
            }
            BigDecimal remaining = woItem.getQty().subtract(woItem.getIssuedQty());
            if (item.qty().compareTo(remaining) > 0) {
                throw of(9_02, 3, String.format("领料超过剩余需求数量（剩余 %s）",
                        remaining.stripTrailingZeros().toPlainString()));
            }
        }
        ErpMrpMaterialReqDO req = new ErpMrpMaterialReqDO();
        req.setNo(serialNoApi.nextNo("material_req"));
        req.setWorkOrderId(workOrderId);
        req.setWarehouseId(warehouseId);
        req.setTotalQty(items.stream().map(Item::qty).reduce(BigDecimal.ZERO, BigDecimal::add));
        req.setRemark(remark);
        req.setStatus(BizOrderStatus.DRAFT.getValue());
        reqMapper.insert(req);
        for (Item item : items) {
            ErpMrpMaterialReqItemDO itemDO = new ErpMrpMaterialReqItemDO();
            itemDO.setReqId(req.getId());
            itemDO.setWorkOrderItemId(item.workOrderItemId());
            itemDO.setProductId(item.productId());
            itemDO.setQty(item.qty());
            itemDO.setRemark(item.remark());
            reqItemMapper.insert(itemDO);
        }
        chatterService.trace(BIZ_TYPE, req.getId(), "创建领料单 " + req.getNo(), null);
        return req.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReq(Long id) {
        ErpMrpMaterialReqDO req = getReq(id);
        OrderStateChecker.require(req.getStatus(), req.getNo(), BizOrderStatus.DRAFT);
        reqMapper.deleteById(id);
        reqItemMapper.delete(new LambdaQueryWrapper<ErpMrpMaterialReqItemDO>()
                .eq(ErpMrpMaterialReqItemDO::getReqId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReq(Long id) {
        ErpMrpMaterialReqDO req = getReq(id);
        OrderStateChecker.require(req.getStatus(), req.getNo(), BizOrderStatus.DRAFT);
        List<ErpMrpMaterialReqItemDO> items = reqItemMapper.selectList(
                new LambdaQueryWrapper<ErpMrpMaterialReqItemDO>()
                        .eq(ErpMrpMaterialReqItemDO::getReqId, id));

        // 1. 库存出账（材料不足时整体回滚）
        List<MoveCommand> commands = new ArrayList<>();
        for (ErpMrpMaterialReqItemDO item : items) {
            commands.add(new MoveCommand(MOVE_BIZ_TYPE, req.getId(), req.getNo(), -1,
                    req.getWarehouseId(), item.getProductId(), item.getQty(), null, null, null));
        }
        stockPostingApi.submit(commands);

        // 2. 回写工单材料行已领数量
        for (ErpMrpMaterialReqItemDO item : items) {
            ErpMrpWorkOrderItemDO woItem = workOrderItemMapper.selectById(item.getWorkOrderItemId());
            if (woItem == null) {
                throw of(9_02, 4, "工单材料行不存在: " + item.getWorkOrderItemId());
            }
            workOrderItemMapper.update(null, new LambdaUpdateWrapper<ErpMrpWorkOrderItemDO>()
                    .eq(ErpMrpWorkOrderItemDO::getId, woItem.getId())
                    .set(ErpMrpWorkOrderItemDO::getIssuedQty, woItem.getIssuedQty().add(item.getQty())));
        }

        // 3. 工单转生产中
        ErpMrpWorkOrderDO order = workOrderMapper.selectById(req.getWorkOrderId());
        if (order != null && BizOrderStatus.AUDITED.getValue().equals(order.getStatus())) {
            workOrderMapper.update(null, new LambdaUpdateWrapper<ErpMrpWorkOrderDO>()
                    .eq(ErpMrpWorkOrderDO::getId, order.getId())
                    .set(ErpMrpWorkOrderDO::getStatus, WorkOrderServiceImpl.STATUS_IN_PRODUCTION));
            chatterService.trace(WorkOrderServiceImpl.BIZ_TYPE, order.getId(),
                    "首次领料，工单转入生产中", null);
        }

        req.setStatus(BizOrderStatus.AUDITED.getValue());
        req.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        req.setAuditTime(LocalDateTime.now());
        reqMapper.updateById(req);
        chatterService.trace(BIZ_TYPE, id,
                String.format("审核领料单 %s，材料已出库", req.getNo()), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidReq(Long id) {
        ErpMrpMaterialReqDO req = getReq(id);
        OrderStateChecker.require(req.getStatus(), req.getNo(), BizOrderStatus.DRAFT);
        req.setStatus(BizOrderStatus.VOIDED.getValue());
        reqMapper.updateById(req);
        chatterService.trace(BIZ_TYPE, id, "作废领料单 " + req.getNo(), null);
    }

    private void validate(Long workOrderId, Long warehouseId, List<Item> items) {
        if (workOrderId == null || warehouseId == null) {
            throw of(9_02, 5, "工单与仓库不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw of(9_02, 6, "领料明细不能为空");
        }
        for (Item item : items) {
            if (item.workOrderItemId() == null || item.qty() == null || item.qty().signum() <= 0) {
                throw of(9_02, 7, "明细中存在材料行/数量不合法的行");
            }
        }
    }

    private ErpMrpMaterialReqDO getReq(Long id) {
        ErpMrpMaterialReqDO req = reqMapper.selectById(id);
        if (req == null) {
            throw of(9_02, 8, "领料单不存在");
        }
        return req;
    }
}
