package cn.tiangong.module.rpt.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.fin.api.FinStatApi;
import cn.tiangong.module.mrp.api.MrpStatApi;
import cn.tiangong.module.pur.api.PurStatApi;
import cn.tiangong.module.sal.api.SalStatApi;
import cn.tiangong.module.stk.api.StockQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 经营驾驶舱：聚合各域统计 API（rpt 不直连任何业务表，全部走 api 包）。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/rpt/dashboard")
public class RptDashboardController {

    private final SalStatApi salStatApi;
    private final PurStatApi purStatApi;
    private final StockQueryApi stockQueryApi;
    private final FinStatApi finStatApi;
    private final MrpStatApi mrpStatApi;

    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPermission('rpt:dashboard:query')")
    public CommonResult<DashboardRespVO> getSummary(@RequestParam(defaultValue = "6") int months) {
        return CommonResult.success(new DashboardRespVO(
                salStatApi.monthlySalesAmount(months),
                purStatApi.monthlyPurchaseAmount(months),
                stockQueryApi.totalStockValue(),
                finStatApi.unsettledReceivable(),
                finStatApi.unsettledPayable(),
                mrpStatApi.activeWorkOrders()));
    }

    public record DashboardRespVO(
            List<Map<String, Object>> salesTrend,
            List<Map<String, Object>> purchaseTrend,
            java.math.BigDecimal stockValue,
            java.math.BigDecimal unsettledReceivable,
            java.math.BigDecimal unsettledPayable,
            long activeWorkOrders) {
    }
}
