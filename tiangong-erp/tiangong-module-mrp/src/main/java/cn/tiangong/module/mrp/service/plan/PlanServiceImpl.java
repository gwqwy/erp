package cn.tiangong.module.mrp.service.plan;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpPlanDO;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpPlanMapper;
import cn.tiangong.module.mrp.service.calc.MrpCalcService;
import cn.tiangong.module.pur.api.PurOrderApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class PlanServiceImpl implements PlanService {

    public static final String BIZ_TYPE = "mrp_plan";
    public static final int STATUS_SAVED = 1;
    public static final int STATUS_CONVERTED = 2;

    private final ErpMrpPlanMapper planMapper;
    private final MrpCalcService calcService;
    private final SerialNoApi serialNoApi;
    private final PurOrderApi purOrderApi;
    private final ChatterService chatterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpMrpPlanDO runAndSave(Long warehouseId, List<MrpCalcService.Demand> demands, String remark) {
        MrpCalcService.CalcResult result = calcService.calc(warehouseId, demands);
        ErpMrpPlanDO plan = new ErpMrpPlanDO();
        plan.setNo(serialNoApi.nextNo("mrp_plan"));
        plan.setWarehouseId(warehouseId);
        plan.setDemandsJson(writeJson(demands));
        plan.setResultJson(writeJson(result.lines()));
        plan.setStatus(STATUS_SAVED);
        plan.setRemark(remark);
        planMapper.insert(plan);
        chatterService.trace(BIZ_TYPE, plan.getId(), "保存 MRP 计划 " + plan.getNo(), null);
        return plan;
    }

    @Override
    public PageResult<ErpMrpPlanDO> getPlanPage(PageParam pageParam, String no, Integer status) {
        LambdaQueryWrapper<ErpMrpPlanDO> wrapper = new LambdaQueryWrapper<ErpMrpPlanDO>()
                .like(no != null && !no.isBlank(), ErpMrpPlanDO::getNo, no)
                .eq(status != null, ErpMrpPlanDO::getStatus, status)
                .orderByDesc(ErpMrpPlanDO::getId);
        return planMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long toPurchaseOrder(Long planId, Long supplierId) {
        ErpMrpPlanDO plan = getPlan(planId);
        if (plan.getStatus() == STATUS_CONVERTED) {
            throw of(9_04, 1, "该计划已转过采购订单");
        }
        List<MrpCalcService.CalcLine> lines = readLines(plan.getResultJson());
        List<MrpCalcService.CalcLine> demands = lines.stream()
                .filter(l -> l.netQty().signum() > 0)
                .toList();
        if (demands.isEmpty()) {
            throw of(9_04, 2, "计划无净需求，无需转采购");
        }
        Long orderId = purOrderApi.createDraftOrder(supplierId,
                "MRP 计划转采购 " + plan.getNo(),
                demands.stream().map(l -> new PurOrderApi.Item(l.productId(), null,
                        l.netQty(), BigDecimal.ZERO, null)).toList());

        plan.setStatus(STATUS_CONVERTED);
        plan.setPurchaseOrderId(orderId);
        planMapper.update(null, new LambdaUpdateWrapper<ErpMrpPlanDO>()
                .eq(ErpMrpPlanDO::getId, planId)
                .set(ErpMrpPlanDO::getStatus, STATUS_CONVERTED)
                .set(ErpMrpPlanDO::getPurchaseOrderId, orderId));
        chatterService.trace(BIZ_TYPE, planId,
                String.format("计划转采购订单，净需求 %d 行，订单 id %s", demands.size(), orderId), null);
        return orderId;
    }

    private ErpMrpPlanDO getPlan(Long id) {
        ErpMrpPlanDO plan = planMapper.selectById(id);
        if (plan == null) {
            throw of(9_04, 3, "MRP 计划不存在");
        }
        return plan;
    }

    @SneakyThrows
    private String writeJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @SneakyThrows
    private List<MrpCalcService.CalcLine> readLines(String json) {
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }
}
