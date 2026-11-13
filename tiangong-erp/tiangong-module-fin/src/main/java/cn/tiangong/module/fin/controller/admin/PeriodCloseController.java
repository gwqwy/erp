package cn.tiangong.module.fin.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePeriodDO;
import cn.tiangong.module.fin.service.PeriodCloseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/fin/period")
public class PeriodCloseController {

    private final PeriodCloseService periodCloseService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('fin:periodclose:operate')")
    public CommonResult<List<ErpFinancePeriodDO>> getPeriodList() {
        return CommonResult.success(periodCloseService.getPeriodList());
    }

    @GetMapping("/is-closed")
    public CommonResult<Boolean> isClosed(@RequestParam("period") String period) {
        return CommonResult.success(periodCloseService.isClosed(period));
    }

    /** 期末结转：损益转本年利润 + 锁账 */
    @PostMapping("/close")
    @PreAuthorize("@ss.hasPermission('fin:periodclose:operate')")
    public CommonResult<String> close(@RequestParam("period") String period) {
        return CommonResult.success(periodCloseService.closePeriod(period,
                cn.tiangong.framework.security.util.SecurityFrameworkUtils.getLoginUser() != null
                        ? cn.tiangong.framework.security.util.SecurityFrameworkUtils.getLoginUser().getUsername() : ""));
    }
}
