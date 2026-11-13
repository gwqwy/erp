package cn.tiangong.module.system.controller.admin.role;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemRoleDO;
import cn.tiangong.module.system.service.role.RoleService;
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
@RequestMapping("/admin-api/system/role")
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<PageResult<RoleRespVO>> getRolePage(PageParam pageParam,
                                                            @RequestParam(required = false) String name) {
        PageResult<SystemRoleDO> page = roleService.getRolePage(pageParam, name);
        return CommonResult.success(PageResult.of(page, RoleController::toRespVO));
    }

    @GetMapping("/list-simple")
    public CommonResult<List<RoleRespVO>> getRoleSimpleList() {
        return CommonResult.success(roleService.getRoleList().stream().map(RoleController::toRespVO).toList());
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:role:create')")
    public CommonResult<Long> createRole(@Valid @RequestBody RoleCreateReqVO reqVO) {
        return CommonResult.success(roleService.createRole(reqVO.name(), reqVO.code(),
                reqVO.sort(), reqVO.remark()));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> updateRole(@Valid @RequestBody RoleUpdateReqVO reqVO) {
        roleService.updateRole(reqVO.id(), reqVO.name(), reqVO.code(), reqVO.sort(),
                reqVO.remark(), reqVO.status());
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> deleteRole(@RequestParam("id") Long id) {
        roleService.deleteRole(id);
        return CommonResult.success(true);
    }

    @GetMapping("/get-menu-ids")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<List<Long>> getRoleMenuIds(@RequestParam("roleId") Long roleId) {
        return CommonResult.success(roleService.getRoleMenuIds(roleId));
    }

    @PutMapping("/assign-menu")
    @PreAuthorize("@ss.hasPermission('system:role:assign-menu')")
    public CommonResult<Boolean> assignRoleMenu(@Valid @RequestBody RoleAssignMenuReqVO reqVO) {
        roleService.assignRoleMenu(reqVO.roleId(), reqVO.menuIds());
        return CommonResult.success(true);
    }

    private static RoleRespVO toRespVO(SystemRoleDO role) {
        return new RoleRespVO(role.getId(), role.getName(), role.getCode(), role.getSort(),
                role.getStatus(), role.getRemark(), role.getCreateTime());
    }

    public record RoleCreateReqVO(@NotBlank(message = "角色名称不能为空") String name,
                                  @NotBlank(message = "角色编码不能为空") String code,
                                  Integer sort, String remark) {
    }

    public record RoleUpdateReqVO(@NotNull(message = "角色编号不能为空") Long id,
                                  String name, String code, Integer sort, String remark, Integer status) {
    }

    public record RoleAssignMenuReqVO(@NotNull(message = "角色编号不能为空") Long roleId,
                                      List<Long> menuIds) {
    }

    public record RoleRespVO(Long id, String name, String code, Integer sort, Integer status,
                             String remark, java.time.LocalDateTime createTime) {
    }
}
