package cn.tiangong.module.fin.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceiptDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceivableDO;
import cn.tiangong.module.fin.service.FinanceLedgerService;
import cn.tiangong.module.fin.service.ReceiptService;
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
@RequestMapping("/admin-api/fin/receipt")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final FinanceLedgerService ledgerService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('fin:receipt:query')")
    public CommonResult<PageResult<ErpFinanceReceiptDO>> getReceiptPage(PageParam pageParam,
                                                                        @RequestParam(required = false) String no,
                                                                        @RequestParam(required = false) Long partnerId,
                                                                        @RequestParam(required = false) Integer status) {
        return CommonResult.success(receiptService.getReceiptPage(pageParam, no, partnerId, status));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('fin:receipt:query')")
    public CommonResult<ReceiptService.ReceiptDetail> getReceiptDetail(@RequestParam("id") Long id) {
        return CommonResult.success(receiptService.getReceiptDetail(id));
    }

    /** 可核销的未结清应收（创建核销明细行下拉用） */
    @GetMapping("/open-receivables")
    @PreAuthorize("@ss.hasPermission('fin:receipt:query')")
    public CommonResult<List<ErpFinanceReceivableDO>> getOpenReceivables(
            @RequestParam(value = "partnerId", required = false) Long partnerId) {
        return CommonResult.success(ledgerService.listOpenReceivables(partnerId));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('fin:receipt:create')")
    public CommonResult<Long> createReceipt(@Valid @RequestBody ReceiptReqVO reqVO) {
        return CommonResult.success(receiptService.createReceipt(reqVO.partnerId(), reqVO.accountId(),
                reqVO.receiptDate(), reqVO.remark(), toItemParams(reqVO.items())));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('fin:receipt:delete')")
    public CommonResult<Boolean> deleteReceipt(@RequestParam("id") Long id) {
        receiptService.deleteReceipt(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('fin:receipt:audit')")
    public CommonResult<Boolean> auditReceipt(@RequestParam("id") Long id) {
        receiptService.auditReceipt(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('fin:receipt:void')")
    public CommonResult<Boolean> voidReceipt(@RequestParam("id") Long id) {
        receiptService.voidReceipt(id);
        return CommonResult.success(true);
    }

    @PostMapping("/reverse")
    @PreAuthorize("@ss.hasPermission('fin:receipt:reverse')")
    public CommonResult<Boolean> reverseReceipt(@RequestParam("id") Long id) {
        receiptService.reverseReceipt(id);
        return CommonResult.success(true);
    }

    private List<ReceiptService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new ReceiptService.Item(
                i.receivableId(), i.amount(), i.remark())).toList();
    }

    public record ItemVO(Long receivableId, @NotNull BigDecimal amount, String remark) {
    }

    public record ReceiptReqVO(@NotNull Long partnerId, @NotNull Long accountId, LocalDate receiptDate,
                               String remark, @NotEmpty List<ItemVO> items) {
    }
}
