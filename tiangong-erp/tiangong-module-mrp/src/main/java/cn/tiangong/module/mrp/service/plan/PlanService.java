package cn.tiangong.module.mrp.service.plan;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpPlanDO;
import cn.tiangong.module.mrp.service.calc.MrpCalcService;

import java.util.List;

public interface PlanService {

    /** 试算并保存计划 */
    ErpMrpPlanDO runAndSave(Long warehouseId, List<MrpCalcService.Demand> demands, String remark);

    PageResult<ErpMrpPlanDO> getPlanPage(PageParam pageParam, String no, Integer status);

    /** 将计划中净需求 > 0 的叶子转成采购订单草稿，回写计划状态 */
    Long toPurchaseOrder(Long planId, Long supplierId);
}
