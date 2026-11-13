package cn.tiangong.module.bas.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.dal.dataobject.ErpProductDO;
import cn.tiangong.module.bas.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/bas/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('bas:product:query')")
    public CommonResult<PageResult<ErpProductDO>> getProductPage(PageParam pageParam,
                                                                 @RequestParam(required = false) String name,
                                                                 @RequestParam(required = false) String no,
                                                                 @RequestParam(required = false) Long categoryId,
                                                                 @RequestParam(required = false) Integer status) {
        return CommonResult.success(productService.getProductPage(pageParam, name, no, categoryId, status));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('bas:product:query')")
    public CommonResult<ErpProductDO> getProduct(@RequestParam("id") Long id) {
        return CommonResult.success(productService.getProduct(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bas:product:create')")
    public CommonResult<Long> createProduct(@Valid @RequestBody ProductReqVO reqVO) {
        return CommonResult.success(productService.createProduct(reqVO.toDO()));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('bas:product:update')")
    public CommonResult<Boolean> updateProduct(@Valid @RequestBody ProductUpdateReqVO reqVO) {
        productService.updateProduct(reqVO.toDO());
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('bas:product:delete')")
    public CommonResult<Boolean> deleteProduct(@RequestParam("id") Long id) {
        productService.deleteProduct(id);
        return CommonResult.success(true);
    }

    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('bas:product:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("status") Integer status) {
        productService.updateProductStatus(id, status);
        return CommonResult.success(true);
    }

    public record ProductReqVO(@NotBlank(message = "产品名称不能为空") String name,
                               Long categoryId, Long unitId, String spec, String barcode,
                               BigDecimal purchasePrice, BigDecimal salePrice, BigDecimal taxRate,
                               BigDecimal stockMin, BigDecimal stockMax,
                               Boolean isBatch, Boolean isSerial, String remark, String no) {
        ErpProductDO toDO() {
            ErpProductDO d = new ErpProductDO();
            d.setNo(no());
            d.setName(name());
            d.setCategoryId(categoryId());
            d.setUnitId(unitId());
            d.setSpec(spec());
            d.setBarcode(barcode());
            d.setPurchasePrice(purchasePrice());
            d.setSalePrice(salePrice());
            d.setTaxRate(taxRate());
            d.setStockMin(stockMin());
            d.setStockMax(stockMax());
            d.setIsBatch(Boolean.TRUE.equals(isBatch()));
            d.setIsSerial(Boolean.TRUE.equals(isSerial()));
            d.setRemark(remark());
            return d;
        }
    }

    public record ProductUpdateReqVO(Long id, String name, Long categoryId, Long unitId, String spec,
                                     String barcode, BigDecimal purchasePrice, BigDecimal salePrice,
                                     BigDecimal taxRate, BigDecimal stockMin, BigDecimal stockMax,
                                     Boolean isBatch, Boolean isSerial, String remark) {
        ErpProductDO toDO() {
            ErpProductDO d = new ErpProductDO();
            d.setId(id());
            d.setName(name());
            d.setCategoryId(categoryId());
            d.setUnitId(unitId());
            d.setSpec(spec());
            d.setBarcode(barcode());
            d.setPurchasePrice(purchasePrice());
            d.setSalePrice(salePrice());
            d.setTaxRate(taxRate());
            d.setStockMin(stockMin());
            d.setStockMax(stockMax());
            d.setIsBatch(isBatch());
            d.setIsSerial(isSerial());
            d.setRemark(remark());
            return d;
        }
    }
}
