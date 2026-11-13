package cn.tiangong.module.mrp.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.ProductApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomDO;
import cn.tiangong.module.mrp.service.bom.BomService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/mrp/bom")
public class MrpBomController {

    private final BomService bomService;
    private final ProductApi productApi;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mrp:bom:query')")
    public CommonResult<PageResult<BomRespVO>> getBomPage(PageParam pageParam,
                                                          @RequestParam(required = false) Long productId,
                                                          @RequestParam(required = false) Integer status) {
        PageResult<ErpMrpBomDO> page = bomService.getBomPage(pageParam, productId, status);
        Map<Long, String> names = productApi.getProductNames(
                page.getList().stream().map(ErpMrpBomDO::getProductId).collect(Collectors.toSet()));
        return CommonResult.success(PageResult.of(page,
                b -> new BomRespVO(String.valueOf(b.getId()), String.valueOf(b.getProductId()),
                        names.getOrDefault(b.getProductId(), "未知产品"), b.getVersion(), b.getQty(),
                        b.getStatus(), b.getIsDefault())));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('mrp:bom:query')")
    public CommonResult<BomService.BomDetail> getBomDetail(@RequestParam("id") Long id) {
        return CommonResult.success(bomService.getBomDetail(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mrp:bom:create')")
    public CommonResult<Long> createBom(@Valid @RequestBody BomReqVO reqVO) {
        return CommonResult.success(bomService.createBom(reqVO.productId(), reqVO.version(),
                reqVO.qty(), reqVO.remark(), toItemParams(reqVO.items())));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('mrp:bom:update')")
    public CommonResult<Boolean> updateBom(@Valid @RequestBody BomUpdateReqVO reqVO) {
        bomService.updateBom(reqVO.id(), reqVO.productId(), reqVO.version(), reqVO.qty(),
                reqVO.remark(), toItemParams(reqVO.items()));
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('mrp:bom:delete')")
    public CommonResult<Boolean> deleteBom(@RequestParam("id") Long id) {
        bomService.deleteBom(id);
        return CommonResult.success(true);
    }

    @PostMapping("/enable")
    @PreAuthorize("@ss.hasPermission('mrp:bom:update')")
    public CommonResult<Boolean> enableBom(@RequestParam("id") Long id) {
        bomService.enableBom(id);
        return CommonResult.success(true);
    }

    private List<BomService.Item> toItemParams(List<ItemVO> items) {
        return items.stream().map(i -> new BomService.Item(
                i.productId(), i.qty(), i.lossRate(), i.remark())).toList();
    }

    public record ItemVO(Long productId, @NotNull BigDecimal qty, BigDecimal lossRate, String remark) {
    }

    public record BomReqVO(@NotNull Long productId, Integer version, @NotNull BigDecimal qty,
                           String remark, @NotEmpty List<ItemVO> items) {
    }

    public record BomUpdateReqVO(@NotNull Long id, @NotNull Long productId, Integer version,
                                 @NotNull BigDecimal qty, String remark, @NotEmpty List<ItemVO> items) {
    }

    public record BomRespVO(String id, String productId, String productName, Integer version,
                            BigDecimal qty, Integer status, Integer isDefault) {
    }
}
