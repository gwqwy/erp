package cn.tiangong.module.sal.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleReturnDO;
import cn.tiangong.module.sal.service.returnorder.SaleReturnService;
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
@RequestMapping("/admin-api/sal/return")
public class SaleReturnController {

    private final SaleReturnService returnService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('sal:return:query')")
    public CommonResult<PageResult<ErpSaleReturnDO>> getReturnPage(PageParam pageParam,
                                                                   @RequestParam(required = false) String no,
                                                                   @RequestParam(required = false) Long customerId,
                                                                   @RequestParam(required = false) Integer status) {
        return CommonResult.success(returnService.getReturnPage(pageParam, no, customerId, status));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('sal:return:query')")
    public CommonResult<SaleReturnService.ReturnDetail> getReturnDetail(@RequestParam("id") Long id) {
        return CommonResult.success(returnService.getReturnDetail(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('sal:return:create')")
    public CommonResult<Long> createReturn(@Valid @RequestBody ReturnReqVO reqVO) {
        return CommonResult.success(returnService.createReturn(reqVO.customerId(), reqVO.warehouseId(),
                reqVO.returnDate(), reqVO.remark(), toItemParams(reqVO.items())));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('sal:return:delete')")
    public CommonResult<Boolean> deleteReturn(@RequestParam("id") Long id) {
        returnService.deleteReturn(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('sal:return:audit')")
    public CommonResult<Boolean> auditReturn(@RequestParam("id") Long id) {
        returnService.auditReturn(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('sal:return:void')")
    public CommonResult<Boolean> voidReturn(@RequestParam("id") Long id) {
        returnService.voidReturn(id);
        return CommonResult.success(true);
    }

    private List<SaleReturnService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new SaleReturnService.Item(
                i.productId(), i.qty(), i.price(), i.remark())).toList();
    }

    public record ItemVO(Long productId, @NotNull BigDecimal qty, @NotNull BigDecimal price, String remark) {
    }

    public record ReturnReqVO(@NotNull Long customerId, @NotNull Long warehouseId,
                              LocalDate returnDate, String remark, @NotEmpty List<ItemVO> items) {
    }
}
