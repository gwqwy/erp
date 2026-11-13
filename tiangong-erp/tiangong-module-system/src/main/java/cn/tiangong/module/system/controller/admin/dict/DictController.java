package cn.tiangong.module.system.controller.admin.dict;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemDictDataDO;
import cn.tiangong.module.system.dal.dataobject.SystemDictTypeDO;
import cn.tiangong.module.system.service.dict.DictService;
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
@RequestMapping("/admin-api/system/dict")
public class DictController {

    private final DictService dictService;

    // ========== 字典类型 ==========

    @GetMapping("/type/page")
    @PreAuthorize("@ss.hasPermission('system:dict:query')")
    public CommonResult<PageResult<SystemDictTypeDO>> getDictTypePage(PageParam pageParam,
                                                                      @RequestParam(required = false) String name) {
        return CommonResult.success(dictService.getDictTypePage(pageParam, name));
    }

    @PostMapping("/type/create")
    @PreAuthorize("@ss.hasPermission('system:dict:create')")
    public CommonResult<Long> createDictType(@RequestParam("name") @NotBlank(message = "字典名称不能为空") String name,
                                             @RequestParam("type") @NotBlank(message = "字典类型不能为空") String type) {
        return CommonResult.success(dictService.createDictType(name, type));
    }

    @PutMapping("/type/update")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    public CommonResult<Boolean> updateDictType(@RequestParam("id") Long id,
                                                @RequestParam("name") @NotBlank(message = "字典名称不能为空") String name) {
        dictService.updateDictType(id, name);
        return CommonResult.success(true);
    }

    @DeleteMapping("/type/delete")
    @PreAuthorize("@ss.hasPermission('system:dict:delete')")
    public CommonResult<Boolean> deleteDictType(@RequestParam("id") Long id) {
        dictService.deleteDictType(id);
        return CommonResult.success(true);
    }

    // ========== 字典数据 ==========

    @GetMapping("/data/list")
    public CommonResult<List<SystemDictDataDO>> getDictDataList(@RequestParam("dictType") String dictType) {
        return CommonResult.success(dictService.getDictDataList(dictType));
    }

    @PostMapping("/data/create")
    @PreAuthorize("@ss.hasPermission('system:dict:create')")
    public CommonResult<Long> createDictData(@RequestParam("dictType") String dictType,
                                             @RequestParam("label") @NotBlank(message = "标签不能为空") String label,
                                             @RequestParam("value") @NotBlank(message = "值不能为空") String value,
                                             @RequestParam(value = "sort", required = false) Integer sort) {
        return CommonResult.success(dictService.createDictData(dictType, label, value, sort));
    }

    @PutMapping("/data/update")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    public CommonResult<Boolean> updateDictData(@RequestParam("id") Long id,
                                                @RequestParam(value = "label", required = false) String label,
                                                @RequestParam(value = "value", required = false) String value,
                                                @RequestParam(value = "sort", required = false) Integer sort) {
        dictService.updateDictData(id, label, value, sort);
        return CommonResult.success(true);
    }

    @DeleteMapping("/data/delete")
    @PreAuthorize("@ss.hasPermission('system:dict:delete')")
    public CommonResult<Boolean> deleteDictData(@RequestParam("id") Long id) {
        dictService.deleteDictData(id);
        return CommonResult.success(true);
    }
}
