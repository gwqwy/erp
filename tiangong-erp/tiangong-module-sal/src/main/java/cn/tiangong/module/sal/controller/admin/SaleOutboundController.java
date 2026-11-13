package cn.tiangong.module.sal.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOutboundDO;
import cn.tiangong.module.sal.service.outbound.SaleOutboundService;
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
@RequestMapping("/admin-api/sal/outbound")
public class SaleOutboundController {

    private final SaleOutboundService outboundService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('sal:outbound:query')")
    public CommonResult<PageResult<ErpSaleOutboundDO>> getOutboundPage(PageParam pageParam,
                                                                       @RequestParam(required = false) String no,
                                                                       @RequestParam(required = false) Long customerId,
                                                                       @RequestParam(required = false) Long orderId,
                                                                       @RequestParam(required = false) Integer status) {
        return CommonResult.success(outboundService.getOutboundPage(pageParam, no, customerId, orderId, status));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('sal:outbound:query')")
    public CommonResult<SaleOutboundService.OutboundDetail> getOutboundDetail(@RequestParam("id") Long id) {
        return CommonResult.success(outboundService.getOutboundDetail(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('sal:outbound:create')")
    public CommonResult<Long> createOutbound(@Valid @RequestBody OutboundReqVO reqVO) {
        return CommonResult.success(outboundService.createOutbound(reqVO.customerId(), reqVO.warehouseId(),
                reqVO.orderId(), reqVO.outboundDate(), reqVO.remark(), toItemParams(reqVO.items())));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('sal:outbound:delete')")
    public CommonResult<Boolean> deleteOutbound(@RequestParam("id") Long id) {
        outboundService.deleteOutbound(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('sal:outbound:audit')")
    public CommonResult<Boolean> auditOutbound(@RequestParam("id") Long id) {
        outboundService.auditOutbound(id);
        return CommonResult.success(true);
    }

    @PostMapping("/submit-approval")
    @PreAuthorize("@ss.hasPermission('sal:outbound:audit')")
    public CommonResult<Boolean> submitApproval(@RequestParam("id") Long id) {
        outboundService.submitApproval(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('sal:outbound:void')")
    public CommonResult<Boolean> voidOutbound(@RequestParam("id") Long id) {
        outboundService.voidOutbound(id);
        return CommonResult.success(true);
    }

    @PostMapping("/reverse")
    @PreAuthorize("@ss.hasPermission('sal:outbound:reverse')")
    public CommonResult<Boolean> reverseOutbound(@RequestParam("id") Long id) {
        outboundService.reverseOutbound(id);
        return CommonResult.success(true);
    }

    private List<SaleOutboundService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new SaleOutboundService.Item(
                i.orderItemId(), i.productId(), i.unitId(), i.qty(), i.price(), i.remark())).toList();
    }

    public record ItemVO(Long orderItemId, Long productId, Long unitId, @NotNull BigDecimal qty,
                         @NotNull BigDecimal price, String remark) {
    }

    public record OutboundReqVO(@NotNull Long customerId, @NotNull Long warehouseId, Long orderId,
                                LocalDate outboundDate, String remark, @NotEmpty List<ItemVO> items) {
    }
}
