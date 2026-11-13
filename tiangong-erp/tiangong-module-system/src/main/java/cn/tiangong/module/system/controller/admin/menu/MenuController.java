package cn.tiangong.module.system.controller.admin.menu;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.system.dal.dataobject.SystemMenuDO;
import cn.tiangong.module.system.service.menu.MenuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/system/menu")
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('system:menu:query')")
    public CommonResult<List<SystemMenuDO>> getMenuList() {
        return CommonResult.success(menuService.getMenuList());
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:menu:create')")
    public CommonResult<Long> createMenu(@Valid @RequestBody MenuReqVO reqVO) {
        return CommonResult.success(menuService.createMenu(toDO(null, reqVO.name(), reqVO.permission(),
                reqVO.type(), reqVO.sort(), reqVO.parentId(), reqVO.path(), reqVO.component(), reqVO.status())));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    public CommonResult<Boolean> updateMenu(@Valid @RequestBody MenuUpdateReqVO reqVO) {
        menuService.updateMenu(toDO(reqVO.id(), reqVO.name(), reqVO.permission(), reqVO.type(),
                reqVO.sort(), reqVO.parentId(), reqVO.path(), reqVO.component(), reqVO.status()));
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<Boolean> deleteMenu(@RequestParam("id") Long id) {
        menuService.deleteMenu(id);
        return CommonResult.success(true);
    }

    private SystemMenuDO toDO(Long id, String name, String permission, Integer type, Integer sort,
                              Long parentId, String path, String component, Integer status) {
        SystemMenuDO menu = new SystemMenuDO();
        menu.setId(id);
        menu.setName(name);
        menu.setPermission(permission);
        menu.setType(type);
        menu.setSort(sort);
        menu.setParentId(parentId);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setStatus(status);
        return menu;
    }

    public record MenuReqVO(@NotBlank(message = "菜单名称不能为空") String name,
                            String permission, @NotNull(message = "菜单类型不能为空") Integer type,
                            Integer sort, Long parentId, String path, String component, Integer status) {
    }

    public record MenuUpdateReqVO(@NotNull(message = "菜单编号不能为空") Long id,
                                  @NotBlank(message = "菜单名称不能为空") String name,
                                  String permission, Integer type, Integer sort, Long parentId,
                                  String path, String component, Integer status) {
    }
}
