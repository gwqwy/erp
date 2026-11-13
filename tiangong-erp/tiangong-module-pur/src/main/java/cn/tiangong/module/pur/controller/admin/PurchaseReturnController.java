package cn.tiangong.module.pur.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseReturnDO;
import cn.tiangong.module.pur.service.returnorder.PurchaseReturnService;
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
@RequestMapping("/admin-api/pur/return")
public class PurchaseReturnController {

    private final PurchaseReturnService returnService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('pur:return:query')")
    public CommonResult<PageResult<ErpPurchaseReturnDO>> getReturnPage(PageParam pageParam,
                                                                       @RequestParam(required = false) String no,
                                                                       @RequestParam(required = false) Long supplierId,
                                                                       @RequestParam(required = false) Integer status) {
        return CommonResult.success(returnService.getReturnPage(pageParam, no, supplierId, status));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('pur:return:query')")
    public CommonResult<PurchaseReturnService.ReturnDetail> getReturnDetail(@RequestParam("id") Long id) {
        return CommonResult.success(returnService.getReturnDetail(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('pur:return:create')")
    public CommonResult<Long> createReturn(@Valid @RequestBody ReturnReqVO reqVO) {
        return CommonResult.success(returnService.createReturn(reqVO.supplierId(), reqVO.warehouseId(),
                reqVO.returnDate(), reqVO.remark(), toItemParams(reqVO.items())));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('pur:return:delete')")
    public CommonResult<Boolean> deleteReturn(@RequestParam("id") Long id) {
        returnService.deleteReturn(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('pur:return:audit')")
    public CommonResult<Boolean> auditReturn(@RequestParam("id") Long id) {
        returnService.auditReturn(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('pur:return:void')")
    public CommonResult<Boolean> voidReturn(@RequestParam("id") Long id) {
        returnService.voidReturn(id);
        return CommonResult.success(true);
    }

    private List<PurchaseReturnService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new PurchaseReturnService.Item(
                i.productId(), i.qty(), i.price(), i.remark())).toList();
    }

    public record ItemVO(Long productId, @NotNull BigDecimal qty, @NotNull BigDecimal price, String remark) {
    }

    public record ReturnReqVO(@NotNull Long supplierId, @NotNull Long warehouseId,
                              LocalDate returnDate, String remark, @NotEmpty List<ItemVO> items) {
    }
}
