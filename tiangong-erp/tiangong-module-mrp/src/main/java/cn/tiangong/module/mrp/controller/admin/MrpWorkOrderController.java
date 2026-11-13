package cn.tiangong.module.mrp.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.ProductApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpWorkOrderDO;
import cn.tiangong.module.mrp.service.order.WorkOrderService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/mrp/work-order")
public class MrpWorkOrderController {

    private final WorkOrderService workOrderService;
    private final ProductApi productApi;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mrp:order:query')")
    public CommonResult<PageResult<WorkOrderRespVO>> getWorkOrderPage(PageParam pageParam,
                                                                      @RequestParam(required = false) String no,
                                                                      @RequestParam(required = false) Long productId,
                                                                      @RequestParam(required = false) Integer status) {
        PageResult<ErpMrpWorkOrderDO> page = workOrderService.getWorkOrderPage(pageParam, no, productId, status);
        Map<Long, String> names = productApi.getProductNames(
                page.getList().stream().map(ErpMrpWorkOrderDO::getProductId).collect(Collectors.toSet()));
        return CommonResult.success(PageResult.of(page,
                o -> new WorkOrderRespVO(String.valueOf(o.getId()), o.getNo(),
                        String.valueOf(o.getProductId()), names.getOrDefault(o.getProductId(), "未知产品"),
                        o.getQty(), o.getFinishedQty(), o.getWarehouseId(), o.getStatus())));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('mrp:order:query')")
    public CommonResult<WorkOrderService.WorkOrderDetail> getWorkOrderDetail(@RequestParam("id") Long id) {
        return CommonResult.success(workOrderService.getWorkOrderDetail(id));
    }

    @GetMapping("/item-list")
    @PreAuthorize("@ss.hasPermission('mrp:order:query')")
    public CommonResult<?> getItemList(@RequestParam("orderId") Long orderId) {
        return CommonResult.success(workOrderService.getItemList(orderId));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mrp:order:create')")
    public CommonResult<Long> createWorkOrder(@RequestParam("productId") Long productId,
                                              @RequestParam("bomId") Long bomId,
                                              @RequestParam("warehouseId") Long warehouseId,
                                              @RequestParam("qty") BigDecimal qty,
                                              @RequestParam(value = "planStartDate", required = false) String planStartDate,
                                              @RequestParam(value = "planFinishDate", required = false) String planFinishDate,
                                              @RequestParam(value = "remark", required = false) String remark) {
        return CommonResult.success(workOrderService.createWorkOrder(productId, bomId, warehouseId, qty,
                planStartDate == null || planStartDate.isBlank() ? null : java.time.LocalDate.parse(planStartDate),
                planFinishDate == null || planFinishDate.isBlank() ? null : java.time.LocalDate.parse(planFinishDate),
                remark));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('mrp:order:delete')")
    public CommonResult<Boolean> deleteWorkOrder(@RequestParam("id") Long id) {
        workOrderService.deleteWorkOrder(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('mrp:order:audit')")
    public CommonResult<Boolean> auditWorkOrder(@RequestParam("id") Long id) {
        workOrderService.auditWorkOrder(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('mrp:order:void')")
    public CommonResult<Boolean> voidWorkOrder(@RequestParam("id") Long id) {
        workOrderService.voidWorkOrder(id);
        return CommonResult.success(true);
    }

    @PostMapping("/finish")
    @PreAuthorize("@ss.hasPermission('mrp:order:finish')")
    public CommonResult<BigDecimal> finishWorkOrder(@RequestParam("id") Long id,
                                                    @RequestParam("qty") BigDecimal qty,
                                                    @RequestParam("warehouseId") Long warehouseId) {
        return CommonResult.success(workOrderService.finishWorkOrder(id, qty, warehouseId));
    }

    public record WorkOrderRespVO(String id, String no, String productId, String productName,
                                  BigDecimal qty, BigDecimal finishedQty, Long warehouseId, Integer status) {
    }
}
