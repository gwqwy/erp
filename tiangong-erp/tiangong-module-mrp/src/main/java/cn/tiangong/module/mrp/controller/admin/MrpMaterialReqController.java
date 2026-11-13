package cn.tiangong.module.mrp.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpMaterialReqDO;
import cn.tiangong.module.mrp.service.req.MaterialReqService;
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
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/mrp/material-req")
public class MrpMaterialReqController {

    private final MaterialReqService reqService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mrp:req:query')")
    public CommonResult<PageResult<ErpMrpMaterialReqDO>> getReqPage(PageParam pageParam,
                                                                    @RequestParam(required = false) String no,
                                                                    @RequestParam(required = false) Long workOrderId,
                                                                    @RequestParam(required = false) Integer status) {
        return CommonResult.success(reqService.getReqPage(pageParam, no, workOrderId, status));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('mrp:req:query')")
    public CommonResult<MaterialReqService.ReqDetail> getReqDetail(@RequestParam("id") Long id) {
        return CommonResult.success(reqService.getReqDetail(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mrp:req:create')")
    public CommonResult<Long> createReq(@Valid @RequestBody ReqReqVO reqVO) {
        return CommonResult.success(reqService.createReq(reqVO.workOrderId(), reqVO.warehouseId(),
                reqVO.remark(), toItemParams(reqVO.items())));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('mrp:req:delete')")
    public CommonResult<Boolean> deleteReq(@RequestParam("id") Long id) {
        reqService.deleteReq(id);
        return CommonResult.success(true);
    }

    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPermission('mrp:req:audit')")
    public CommonResult<Boolean> auditReq(@RequestParam("id") Long id) {
        reqService.auditReq(id);
        return CommonResult.success(true);
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('mrp:req:void')")
    public CommonResult<Boolean> voidReq(@RequestParam("id") Long id) {
        reqService.voidReq(id);
        return CommonResult.success(true);
    }

    private List<MaterialReqService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new MaterialReqService.Item(
                i.workOrderItemId(), i.productId(), i.qty(), i.remark())).toList();
    }

    public record ItemVO(Long workOrderItemId, Long productId, @NotNull BigDecimal qty, String remark) {
    }

    public record ReqReqVO(@NotNull Long workOrderId, @NotNull Long warehouseId,
                           String remark, @NotEmpty List<ItemVO> items) {
    }
}
