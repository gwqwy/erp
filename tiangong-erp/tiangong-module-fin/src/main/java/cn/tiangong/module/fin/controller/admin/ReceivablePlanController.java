package cn.tiangong.module.fin.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.fin.dal.dataobject.ErpReceivablePlanDO;
import cn.tiangong.module.fin.dal.mysql.ErpReceivablePlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 回款计划（登记计划 → 回款确认）。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/fin/receivable-plan")
public class ReceivablePlanController {

    private final cn.tiangong.module.fin.dal.mysql.ErpReceivablePlanMapper planMapper;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('fin:receivableplan:query')")
    public CommonResult<List<ErpReceivablePlanDO>> getList(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(planMapper.selectList(new LambdaQueryWrapper<ErpReceivablePlanDO>()
                .eq(partnerId != null, ErpReceivablePlanDO::getPartnerId, partnerId)
                .eq(status != null, ErpReceivablePlanDO::getStatus, status)
                .orderByAsc(ErpReceivablePlanDO::getPlanDate)));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('fin:receivableplan:create')")
    public CommonResult<Long> create(@RequestParam("partnerId") @NotNull Long partnerId,
                                     @RequestParam("planDate") @NotNull String planDate,
                                     @RequestParam("amount") @NotNull BigDecimal amount,
                                     @RequestParam(value = "remark", required = false) String remark) {
        ErpReceivablePlanDO plan = new ErpReceivablePlanDO();
        plan.setNo("HK" + System.currentTimeMillis() % 10000000000L);
        plan.setPartnerId(partnerId);
        plan.setPlanDate(LocalDate.parse(planDate));
        plan.setAmount(amount);
        plan.setStatus(0);
        plan.setRemark(remark);
        planMapper.insert(plan);
        return CommonResult.success(plan.getId());
    }

    /** 标记已回款 */
    @PostMapping("/done")
    @PreAuthorize("@ss.hasPermission('fin:receivableplan:create')")
    public CommonResult<Boolean> done(@RequestParam("id") Long id) {
        planMapper.update(null, new LambdaUpdateWrapper<ErpReceivablePlanDO>()
                .eq(ErpReceivablePlanDO::getId, id)
                .set(ErpReceivablePlanDO::getStatus, 1));
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('fin:receivableplan:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        planMapper.deleteById(id);
        return CommonResult.success(true);
    }
}
