package cn.tiangong.module.bas.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.bas.dal.dataobject.ErpProductCategoryDO;
import cn.tiangong.module.bas.service.ProductCategoryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/bas/product-category")
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    @GetMapping("/list")
    public CommonResult<List<ErpProductCategoryDO>> getCategoryList(
            @RequestParam(value = "status", required = false) Integer status) {
        return CommonResult.success(categoryService.getCategoryList(status));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bas:product-category:create')")
    public CommonResult<Long> createCategory(@RequestParam("name") @NotBlank(message = "分类名称不能为空") String name,
                                             @RequestParam(value = "parentId", required = false) Long parentId,
                                             @RequestParam(value = "sort", required = false) Integer sort) {
        return CommonResult.success(categoryService.createCategory(name, parentId, sort));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('bas:product-category:update')")
    public CommonResult<Boolean> updateCategory(@RequestParam("id") Long id,
                                                @RequestParam(value = "name", required = false) String name,
                                                @RequestParam(value = "parentId", required = false) Long parentId,
                                                @RequestParam(value = "sort", required = false) Integer sort,
                                                @RequestParam(value = "status", required = false) Integer status) {
        categoryService.updateCategory(id, name, parentId, sort, status);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('bas:product-category:delete')")
    public CommonResult<Boolean> deleteCategory(@RequestParam("id") Long id) {
        categoryService.deleteCategory(id);
        return CommonResult.success(true);
    }
}
