package cn.tiangong.module.fin.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePaymentDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePayableDO;
import cn.tiangong.module.fin.service.FinanceLedgerService;
import cn.tiangong.module.fin.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/fin/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final FinanceLedgerService ledgerService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('fin:payment:query')")
    public CommonResult<PageResult<ErpFinancePaymentDO>> getPaymentPage(PageParam pageParam,
                                                                        @RequestParam(required = false) String no,
                                                                        @RequestParam(required = false) Long partnerId,
                                                                        @RequestParam(required = false) Integer status) {
        return CommonResult.success(paymentService.getPaymentPage(pageParam, no, partnerId, status));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('fin:payment:query')")
    public CommonResult<PaymentService.PaymentDetail> getPaymentDetail(@RequestParam("id") Long id) {
        return CommonResult.success(paymentService.getPaymentDetail(id));
    }

    @GetMapping("/open-payables")
    @PreAuthorize("@ss.hasPermission('fin:payment:query')")
    public CommonResult<List<ErpFinancePayableDO>> getOpenPayables(
            @RequestParam(value = "partnerId", required = false) Long partnerId) {
        return CommonResult.success(ledgerService.listOpenPayables(partnerId));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('fin:payment:create')")
    public CommonResult<Long> createPayment(@Valid @RequestBody PaymentReqVO reqVO) {
        return CommonResult.success(paymentService.createPayment(reqVO.partnerId(), reqVO.accountId(),
                reqVO.paymentDate(), reqVO.remark(), toItemParams(reqVO.items())));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('fin:payment:delete')")
    public CommonResult<Boolean> deletePayment(@RequestParam("id") Long id) {
        paymentService.deletePayment(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('fin:payment:audit')")
    public CommonResult<Boolean> auditPayment(@RequestParam("id") Long id) {
        paymentService.auditPayment(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('fin:payment:void')")
    public CommonResult<Boolean> voidPayment(@RequestParam("id") Long id) {
        paymentService.voidPayment(id);
        return CommonResult.success(true);
    }

    @PostMapping("/reverse")
    @PreAuthorize("@ss.hasPermission('fin:payment:reverse')")
    public CommonResult<Boolean> reversePayment(@RequestParam("id") Long id) {
        paymentService.reversePayment(id);
        return CommonResult.success(true);
    }

    private List<PaymentService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new PaymentService.Item(
                i.payableId(), i.amount(), i.remark())).toList();
    }

    public record ItemVO(Long payableId, @NotNull BigDecimal amount, String remark) {
    }

    public record PaymentReqVO(@NotNull Long partnerId, @NotNull Long accountId, LocalDate paymentDate,
                               String remark, @NotEmpty List<ItemVO> items) {
    }
}
