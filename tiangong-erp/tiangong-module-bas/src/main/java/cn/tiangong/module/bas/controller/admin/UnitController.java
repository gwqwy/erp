package cn.tiangong.module.bas.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.bas.dal.dataobject.ErpUnitDO;
import cn.tiangong.module.bas.service.UnitService;
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
@RequestMapping("/admin-api/bas/unit")
public class UnitController {

    private final UnitService unitService;

    @GetMapping("/list")
    public CommonResult<List<ErpUnitDO>> getUnitList(
            @RequestParam(value = "status", required = false) Integer status) {
        return CommonResult.success(unitService.getUnitList(status));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bas:unit:create')")
    public CommonResult<Long> createUnit(@RequestParam("name") @NotBlank(message = "单位名称不能为空") String name) {
        return CommonResult.success(unitService.createUnit(name));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('bas:unit:update')")
    public CommonResult<Boolean> updateUnit(@RequestParam("id") Long id,
                                            @RequestParam(value = "name", required = false) String name,
                                            @RequestParam(value = "status", required = false) Integer status) {
        unitService.updateUnit(id, name, status);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('bas:unit:delete')")
    public CommonResult<Boolean> deleteUnit(@RequestParam("id") Long id) {
        unitService.deleteUnit(id);
        return CommonResult.success(true);
    }
}
