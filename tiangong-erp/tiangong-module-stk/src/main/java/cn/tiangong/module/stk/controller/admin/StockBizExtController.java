package cn.tiangong.module.stk.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.stk.dal.dataobject.StockCheckDO;
import cn.tiangong.module.stk.dal.dataobject.StockOtherDO;
import cn.tiangong.module.stk.dal.dataobject.StockTransferDO;
import cn.tiangong.module.stk.service.StockBizExtService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 库存扩展业务：调拨 / 盘点 / 其他出入库。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/stk")
public class StockBizExtController {

    private final StockBizExtService extService;

    // ========== 调拨 ==========

    @GetMapping("/transfer/page")
    @PreAuthorize("@ss.hasPermission('stk:transfer:query')")
    public CommonResult<PageResult<StockTransferDO>> getTransferPage(PageParam pageParam,
                                                                     @RequestParam(required = false) String no,
                                                                     @RequestParam(required = false) Integer status) {
        return CommonResult.success(extService.getTransferPage(pageParam, no, status));
    }

    @GetMapping("/transfer/detail")
    @PreAuthorize("@ss.hasPermission('stk:transfer:query')")
    public CommonResult<StockBizExtService.TransferDetail> getTransferDetail(@RequestParam("id") Long id) {
        return CommonResult.success(extService.getTransferDetail(id));
    }

    public record TransferItemReq(Long productId, BigDecimal qty, String remark) {
    }

    @PostMapping("/transfer/create")
    @PreAuthorize("@ss.hasPermission('stk:transfer:create')")
    public CommonResult<Long> createTransfer(@RequestParam("fromWarehouseId") Long fromWarehouseId,
                                             @RequestParam("toWarehouseId") Long toWarehouseId,
                                             @RequestParam(value = "transferDate", required = false) String transferDate,
                                             @RequestParam(value = "remark", required = false) String remark,
                                             @RequestBody List<TransferItemReq> items) {
        List<StockBizExtService.TransferItem> list = items.stream()
                .map(i -> new StockBizExtService.TransferItem(i.productId(), i.qty(), i.remark())).toList();
        return CommonResult.success(extService.createTransferEx(fromWarehouseId, toWarehouseId,
                transferDate == null || transferDate.isBlank() ? null : LocalDate.parse(transferDate), remark, list));
    }

    @DeleteMapping("/transfer/delete")
    @PreAuthorize("@ss.hasPermission('stk:transfer:delete')")
    public CommonResult<Boolean> deleteTransfer(@RequestParam("id") Long id) {
        extService.deleteTransfer(id);
        return CommonResult.success(true);
    }

    @PostMapping("/transfer/audit")
    @PreAuthorize("@ss.hasPermission('stk:transfer:audit')")
    public CommonResult<Boolean> auditTransfer(@RequestParam("id") Long id) {
        extService.auditTransfer(id);
        return CommonResult.success(true);
    }

    @PostMapping("/transfer/void")
    @PreAuthorize("@ss.hasPermission('stk:transfer:void')")
    public CommonResult<Boolean> voidTransfer(@RequestParam("id") Long id) {
        extService.voidTransfer(id);
        return CommonResult.success(true);
    }

    // ========== 盘点 ==========

    @GetMapping("/check/page")
    @PreAuthorize("@ss.hasPermission('stk:check:query')")
    public CommonResult<PageResult<StockCheckDO>> getCheckPage(PageParam pageParam,
                                                               @RequestParam(required = false) String no,
                                                               @RequestParam(required = false) Integer status) {
        return CommonResult.success(extService.getCheckPage(pageParam, no, status));
    }

    @GetMapping("/check/detail")
    @PreAuthorize("@ss.hasPermission('stk:check:query')")
    public CommonResult<StockBizExtService.CheckDetail> getCheckDetail(@RequestParam("id") Long id) {
        return CommonResult.success(extService.getCheckDetail(id));
    }

    @PostMapping("/check/create")
    @PreAuthorize("@ss.hasPermission('stk:check:create')")
    public CommonResult<Long> createCheck(@RequestParam("warehouseId") Long warehouseId,
                                          @RequestParam(value = "remark", required = false) String remark) {
        return CommonResult.success(extService.createCheck(warehouseId, remark));
    }

    @PutMapping("/check/update-real-qty")
    @PreAuthorize("@ss.hasPermission('stk:check:create')")
    public CommonResult<Boolean> updateRealQty(@RequestParam("itemId") Long itemId,
                                               @RequestParam("realQty") BigDecimal realQty) {
        extService.updateRealQty(itemId, realQty);
        return CommonResult.success(true);
    }

    @DeleteMapping("/check/delete")
    @PreAuthorize("@ss.hasPermission('stk:check:delete')")
    public CommonResult<Boolean> deleteCheck(@RequestParam("id") Long id) {
        extService.deleteCheck(id);
        return CommonResult.success(true);
    }

    @PostMapping("/check/audit")
    @PreAuthorize("@ss.hasPermission('stk:check:audit')")
    public CommonResult<Boolean> auditCheck(@RequestParam("id") Long id) {
        extService.auditCheck(id);
        return CommonResult.success(true);
    }

    // ========== 其他出入库 ==========

    @GetMapping("/other/page")
    @PreAuthorize("@ss.hasPermission('stk:other:query')")
    public CommonResult<PageResult<StockOtherDO>> getOtherPage(PageParam pageParam,
                                                               @RequestParam(required = false) String no,
                                                               @RequestParam(required = false) String otherType,
                                                               @RequestParam(required = false) Integer status) {
        return CommonResult.success(extService.getOtherPage(pageParam, no, otherType, status));
    }

    @GetMapping("/other/detail")
    @PreAuthorize("@ss.hasPermission('stk:other:query')")
    public CommonResult<StockBizExtService.OtherDetail> getOtherDetail(@RequestParam("id") Long id) {
        return CommonResult.success(extService.getOtherDetail(id));
    }

    public record OtherItemReq(Long productId, BigDecimal qty, BigDecimal price, String remark) {
    }

    @PostMapping("/other/create")
    @PreAuthorize("@ss.hasPermission('stk:other:create')")
    public CommonResult<Long> createOther(@RequestParam("otherType") String otherType,
                                          @RequestParam("warehouseId") Long warehouseId,
                                          @RequestParam(value = "otherDate", required = false) String otherDate,
                                          @RequestParam(value = "remark", required = false) String remark,
                                          @RequestBody List<OtherItemReq> items) {
        List<StockBizExtService.OtherItem> list = items.stream()
                .map(i -> new StockBizExtService.OtherItem(i.productId(), i.qty(), i.price(), i.remark())).toList();
        return CommonResult.success(extService.createOther(otherType, warehouseId,
                otherDate == null || otherDate.isBlank() ? null : LocalDate.parse(otherDate), remark, list));
    }

    @DeleteMapping("/other/delete")
    @PreAuthorize("@ss.hasPermission('stk:other:delete')")
    public CommonResult<Boolean> deleteOther(@RequestParam("id") Long id) {
        extService.deleteOther(id);
        return CommonResult.success(true);
    }

    @PostMapping("/other/audit")
    @PreAuthorize("@ss.hasPermission('stk:other:audit')")
    public CommonResult<Boolean> auditOther(@RequestParam("id") Long id) {
        extService.auditOther(id);
        return CommonResult.success(true);
    }

    @PostMapping("/other/void")
    @PreAuthorize("@ss.hasPermission('stk:other:audit')")
    public CommonResult<Boolean> voidOther(@RequestParam("id") Long id) {
        extService.voidOther(id);
        return CommonResult.success(true);
    }

    // ========== 预警 ==========

    @GetMapping("/alert/list")
    @PreAuthorize("@ss.hasPermission('stk:quant:query')")
    public CommonResult<List<AlertVO>> getAlertList() {
        return CommonResult.success(extService.stockAlertList());
    }

    @NotNull
    public record AlertVO(String productId, String productName, BigDecimal totalQty,
                          BigDecimal stockMin, BigDecimal stockMax, String level) {
    }
}
