package cn.tiangong.module.mrp.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.mrp.service.calc.MrpCalcService;
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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/mrp/calc")
public class MrpCalcController {

    private final MrpCalcService calcService;
    private final cn.tiangong.module.mrp.service.cost.BomCostService bomCostService;

    @PostMapping("/run")
    @PreAuthorize("@ss.hasPermission('mrp:calc:query')")
    public CommonResult<MrpCalcService.CalcResult> run(@Valid @RequestBody CalcReqVO reqVO) {
        return CommonResult.success(calcService.calc(reqVO.warehouseId(), toItemParams(reqVO.demands())));
    }

    private List<MrpCalcService.Demand> toItemParams(List<DemandVO> demands) {
        return demands.stream().map(d -> new MrpCalcService.Demand(d.productId(), d.qty())).toList();
    }

    public record DemandVO(@NotNull Long productId, @NotNull BigDecimal qty) {
    }

    public record CalcReqVO(@NotNull Long warehouseId, @NotEmpty List<DemandVO> demands) {
    }

    /** BOM 成本卷算（默认 BOM 递归 + 叶子采购价） */
    @GetMapping("/cost")
    @PreAuthorize("@ss.hasPermission('mrp:calc:query')")
    public CommonResult<cn.tiangong.module.mrp.service.cost.BomCostService.RollupResult> cost(
            @org.springframework.web.bind.annotation.RequestParam("productId") Long productId) {
        return CommonResult.success(bomCostService.rollup(productId));
    }
}
