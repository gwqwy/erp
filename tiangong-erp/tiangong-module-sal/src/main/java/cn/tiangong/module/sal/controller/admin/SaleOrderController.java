package cn.tiangong.module.sal.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOrderDO;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOrderItemDO;
import cn.tiangong.module.sal.service.order.SaleOrderService;
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
@RequestMapping("/admin-api/sal/order")
public class SaleOrderController {

    private final SaleOrderService orderService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('sal:order:query')")
    public CommonResult<PageResult<ErpSaleOrderDO>> getOrderPage(PageParam pageParam,
                                                                 @RequestParam(required = false) String no,
                                                                 @RequestParam(required = false) Long customerId,
                                                                 @RequestParam(required = false) Integer status) {
        return CommonResult.success(orderService.getOrderPage(pageParam, no, customerId, status));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('sal:order:query')")
    public CommonResult<SaleOrderService.OrderDetail> getOrderDetail(@RequestParam("id") Long id) {
        return CommonResult.success(orderService.getOrderDetail(id));
    }

    @GetMapping("/item-list")
    @PreAuthorize("@ss.hasPermission('sal:order:query')")
    public CommonResult<List<ErpSaleOrderItemDO>> getItemList(@RequestParam("orderId") Long orderId) {
        return CommonResult.success(orderService.getItemList(orderId));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('sal:order:create')")
    public CommonResult<Long> createOrder(@Valid @RequestBody OrderReqVO reqVO) {
        return CommonResult.success(orderService.createOrder(reqVO.customerId(), reqVO.orderDate(),
                reqVO.expectDate(), reqVO.remark(), toItemParams(reqVO.items())));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('sal:order:update')")
    public CommonResult<Boolean> updateOrder(@Valid @RequestBody OrderUpdateReqVO reqVO) {
        orderService.updateOrder(reqVO.id(), reqVO.customerId(), reqVO.orderDate(),
                reqVO.expectDate(), reqVO.remark(), toItemParams(reqVO.items()));
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('sal:order:delete')")
    public CommonResult<Boolean> deleteOrder(@RequestParam("id") Long id) {
        orderService.deleteOrder(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('sal:order:audit')")
    public CommonResult<Boolean> auditOrder(@RequestParam("id") Long id) {
        orderService.auditOrder(id);
        return CommonResult.success(true);
    }

    @PostMapping("/submit-approval")
    @PreAuthorize("@ss.hasPermission('sal:order:submit')")
    public CommonResult<Boolean> submitApproval(@RequestParam("id") Long id) {
        orderService.submitApproval(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('sal:order:void')")
    public CommonResult<Boolean> voidOrder(@RequestParam("id") Long id) {
        orderService.voidOrder(id);
        return CommonResult.success(true);
    }

    private List<SaleOrderService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new SaleOrderService.Item(
                i.productId(), i.unitId(), i.qty(), i.price(), i.taxRate(), i.remark())).toList();
    }

    public record ItemVO(Long productId, Long unitId, @NotNull BigDecimal qty,
                         @NotNull BigDecimal price, BigDecimal taxRate, String remark) {
    }

    public record OrderReqVO(@NotNull Long customerId, LocalDate orderDate, LocalDate expectDate,
                             String remark, @NotEmpty List<ItemVO> items) {
    }

    public record OrderUpdateReqVO(@NotNull Long id, @NotNull Long customerId, LocalDate orderDate,
                                   LocalDate expectDate, String remark, @NotEmpty List<ItemVO> items) {
    }
}
