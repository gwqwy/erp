package cn.tiangong.module.pur.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseInboundDO;
import cn.tiangong.module.pur.service.inbound.PurchaseInboundService;
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
@RequestMapping("/admin-api/pur/inbound")
public class PurchaseInboundController {

    private final PurchaseInboundService inboundService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('pur:inbound:query')")
    public CommonResult<PageResult<ErpPurchaseInboundDO>> getInboundPage(PageParam pageParam,
                                                                         @RequestParam(required = false) String no,
                                                                         @RequestParam(required = false) Long supplierId,
                                                                         @RequestParam(required = false) Long orderId,
                                                                         @RequestParam(required = false) Integer status) {
        return CommonResult.success(inboundService.getInboundPage(pageParam, no, supplierId, orderId, status));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('pur:inbound:query')")
    public CommonResult<PurchaseInboundService.InboundDetail> getInboundDetail(@RequestParam("id") Long id) {
        return CommonResult.success(inboundService.getInboundDetail(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('pur:inbound:create')")
    public CommonResult<Long> createInbound(@Valid @RequestBody InboundReqVO reqVO) {
        return CommonResult.success(inboundService.createInbound(reqVO.supplierId(), reqVO.warehouseId(),
                reqVO.orderId(), reqVO.inboundDate(), reqVO.remark(), toItemParams(reqVO.items())));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('pur:inbound:delete')")
    public CommonResult<Boolean> deleteInbound(@RequestParam("id") Long id) {
        inboundService.deleteInbound(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('pur:inbound:audit')")
    public CommonResult<Boolean> auditInbound(@RequestParam("id") Long id) {
        inboundService.auditInbound(id);
        return CommonResult.success(true);
    }

    @PostMapping("/submit-approval")
    @PreAuthorize("@ss.hasPermission('pur:inbound:audit')")
    public CommonResult<Boolean> submitApproval(@RequestParam("id") Long id) {
        inboundService.submitApproval(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('pur:inbound:void')")
    public CommonResult<Boolean> voidInbound(@RequestParam("id") Long id) {
        inboundService.voidInbound(id);
        return CommonResult.success(true);
    }

    @PostMapping("/reverse")
    @PreAuthorize("@ss.hasPermission('pur:inbound:reverse')")
    public CommonResult<Boolean> reverseInbound(@RequestParam("id") Long id) {
        inboundService.reverseInbound(id);
        return CommonResult.success(true);
    }

    private List<PurchaseInboundService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new PurchaseInboundService.Item(
                i.orderItemId(), i.productId(), i.unitId(), i.qty(), i.price(), i.remark())).toList();
    }

    public record ItemVO(Long orderItemId, Long productId, Long unitId, @NotNull BigDecimal qty,
                         @NotNull BigDecimal price, String remark) {
    }

    public record InboundReqVO(@NotNull Long supplierId, @NotNull Long warehouseId, Long orderId,
                               LocalDate inboundDate, String remark, @NotEmpty List<ItemVO> items) {
    }
}
