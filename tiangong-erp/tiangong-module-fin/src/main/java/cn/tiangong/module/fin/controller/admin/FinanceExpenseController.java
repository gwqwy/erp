package cn.tiangong.module.fin.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceExpenseDO;
import cn.tiangong.module.fin.service.ExpenseService;
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

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/fin/expense")
public class FinanceExpenseController {

    private final ExpenseService expenseService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('fin:expense:query')")
    public CommonResult<PageResult<ErpFinanceExpenseDO>> getExpensePage(PageParam pageParam,
                                                                        @RequestParam(required = false) String no,
                                                                        @RequestParam(required = false) String category,
                                                                        @RequestParam(required = false) Integer status) {
        return CommonResult.success(expenseService.getExpensePage(pageParam, no, category, status));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('fin:expense:create')")
    public CommonResult<Long> createExpense(@RequestParam("category") @NotBlank String category,
                                            @RequestParam("amount") @NotNull BigDecimal amount,
                                            @RequestParam("accountId") @NotNull Long accountId,
                                            @RequestParam(value = "expenseDate", required = false) String expenseDate,
                                            @RequestParam(value = "remark", required = false) String remark) {
        return CommonResult.success(expenseService.createExpense(category, amount, accountId,
                expenseDate == null || expenseDate.isBlank() ? null : LocalDate.parse(expenseDate), remark));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('fin:expense:delete')")
    public CommonResult<Boolean> deleteExpense(@RequestParam("id") Long id) {
        expenseService.deleteExpense(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('fin:expense:audit')")
    public CommonResult<Boolean> auditExpense(@RequestParam("id") Long id) {
        expenseService.auditExpense(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('fin:expense:audit')")
    public CommonResult<Boolean> voidExpense(@RequestParam("id") Long id) {
        expenseService.voidExpense(id);
        return CommonResult.success(true);
    }
}
