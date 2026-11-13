package cn.tiangong.module.mrp.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpPlanDO;
import cn.tiangong.module.mrp.service.calc.MrpCalcService;
import cn.tiangong.module.mrp.service.plan.PlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/mrp/plan")
public class MrpPlanController {

    private final PlanService planService;

    @PostMapping("/run-and-save")
    @PreAuthorize("@ss.hasPermission('mrp:plan:create')")
    public CommonResult<ErpMrpPlanDO> runAndSave(@Valid @RequestBody RunReqVO reqVO) {
        List<MrpCalcService.Demand> demands = reqVO.demands().stream()
                .map(d -> new MrpCalcService.Demand(d.productId(), d.qty())).toList();
        return CommonResult.success(planService.runAndSave(reqVO.warehouseId(), demands, reqVO.remark()));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mrp:plan:query')")
    public CommonResult<PageResult<ErpMrpPlanDO>> getPlanPage(PageParam pageParam,
                                                              @RequestParam(required = false) String no,
                                                              @RequestParam(required = false) Integer status) {
        return CommonResult.success(planService.getPlanPage(pageParam, no, status));
    }

    @PostMapping("/to-purchase-order")
    @PreAuthorize("@ss.hasPermission('mrp:plan:convert')")
    public CommonResult<Long> toPurchaseOrder(@RequestParam("id") Long id,
                                              @RequestParam("supplierId") Long supplierId) {
        return CommonResult.success(planService.toPurchaseOrder(id, supplierId));
    }

    public record DemandVO(@NotNull Long productId, @NotNull BigDecimal qty) {
    }

    public record RunReqVO(@NotNull Long warehouseId, @NotEmpty List<DemandVO> demands, String remark) {
    }
}
