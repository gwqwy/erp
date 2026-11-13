package cn.tiangong.module.fin.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceInvoiceDO;
import cn.tiangong.module.fin.service.InvoiceService;
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

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/fin/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('fin:invoice:query')")
    public CommonResult<List<ErpFinanceInvoiceDO>> getList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String invoiceNo) {
        return CommonResult.success(invoiceService.getList(type, invoiceNo));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('fin:invoice:create')")
    public CommonResult<Long> create(@RequestParam("type") @NotBlank String type,
                                     @RequestParam("invoiceNo") @NotBlank String invoiceNo,
                                     @RequestParam(value = "invoiceDate", required = false) String invoiceDate,
                                     @RequestParam(value = "orderId", required = false) Long orderId,
                                     @RequestParam("amount") @NotNull BigDecimal amount,
                                     @RequestParam(value = "taxAmount", required = false) BigDecimal taxAmount,
                                     @RequestParam(value = "remark", required = false) String remark) {
        return CommonResult.success(invoiceService.create(type, invoiceNo,
                invoiceDate == null || invoiceDate.isBlank() ? null : LocalDate.parse(invoiceDate),
                orderId, amount, taxAmount, remark));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('fin:invoice:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        invoiceService.delete(id);
        return CommonResult.success(true);
    }
}
