package cn.tiangong.module.pur.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderDO;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderItemDO;
import cn.tiangong.module.pur.service.order.PurchaseOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/pur/order")
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('pur:order:query')")
    public CommonResult<PageResult<ErpPurchaseOrderDO>> getOrderPage(PageParam pageParam,
                                                                     @RequestParam(required = false) String no,
                                                                     @RequestParam(required = false) Long supplierId,
                                                                     @RequestParam(required = false) Integer status) {
        return CommonResult.success(orderService.getOrderPage(pageParam, no, supplierId, status));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('pur:order:query')")
    public CommonResult<PurchaseOrderService.OrderDetail> getOrderDetail(@RequestParam("id") Long id) {
        return CommonResult.success(orderService.getOrderDetail(id));
    }

    @GetMapping("/item-list")
    @PreAuthorize("@ss.hasPermission('pur:order:query')")
    public CommonResult<List<ErpPurchaseOrderItemDO>> getItemList(@RequestParam("orderId") Long orderId) {
        return CommonResult.success(orderService.getItemList(orderId));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('pur:order:create')")
    public CommonResult<Long> createOrder(@Valid @RequestBody OrderReqVO reqVO) {
        return CommonResult.success(orderService.createOrder(reqVO.supplierId(), reqVO.orderDate(),
                reqVO.expectDate(), reqVO.remark(), toItemParams(reqVO.items())));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('pur:order:update')")
    public CommonResult<Boolean> updateOrder(@Valid @RequestBody OrderUpdateReqVO reqVO) {
        orderService.updateOrder(reqVO.id(), reqVO.supplierId(), reqVO.orderDate(),
                reqVO.expectDate(), reqVO.remark(), toItemParams(reqVO.items()));
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('pur:order:delete')")
    public CommonResult<Boolean> deleteOrder(@RequestParam("id") Long id) {
        orderService.deleteOrder(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('pur:order:audit')")
    public CommonResult<Boolean> auditOrder(@RequestParam("id") Long id) {
        orderService.auditOrder(id);
        return CommonResult.success(true);
    }

    @PostMapping("/submit-approval")
    @PreAuthorize("@ss.hasPermission('pur:order:submit')")
    public CommonResult<Boolean> submitApproval(@RequestParam("id") Long id) {
        orderService.submitApproval(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('pur:order:void')")
    public CommonResult<Boolean> voidOrder(@RequestParam("id") Long id) {
        orderService.voidOrder(id);
        return CommonResult.success(true);
    }

    private List<PurchaseOrderService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new PurchaseOrderService.Item(
                i.productId(), i.unitId(), i.qty(), i.price(), i.taxRate(), i.remark())).toList();
    }

    public record ItemVO(Long productId, Long unitId, @NotNull BigDecimal qty,
                         @NotNull BigDecimal price, BigDecimal taxRate, String remark) {
    }

    public record OrderReqVO(@NotNull Long supplierId, LocalDate orderDate, LocalDate expectDate,
                             String remark, @NotEmpty List<ItemVO> items) {
    }

    public record OrderUpdateReqVO(@NotNull Long id, @NotNull Long supplierId, LocalDate orderDate,
                                   LocalDate expectDate, String remark, @NotEmpty List<ItemVO> items) {
    }
}
