package cn.tiangong.module.system.controller.admin.user;

import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemUsersDO;
import cn.tiangong.module.system.service.user.UserService;
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
@RequestMapping("/admin-api/system/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<PageResult<UserRespVO>> getUserPage(PageParam pageParam,
                                                            @RequestParam(required = false) String username,
                                                            @RequestParam(required = false) String nickname) {
        PageResult<SystemUsersDO> page = userService.getUserPage(pageParam, username, nickname);
        return CommonResult.success(PageResult.of(page, this::toRespVO));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        return CommonResult.success(toRespVO(userService.getUser(id)));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> createUser(@Valid @RequestBody UserCreateReqVO reqVO) {
        return CommonResult.success(userService.createUser(reqVO.username(), reqVO.password(),
                reqVO.nickname(), reqVO.deptId()));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserUpdateReqVO reqVO) {
        userService.updateUser(reqVO.id(), reqVO.nickname(), reqVO.deptId(), reqVO.status());
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        if (id.equals(SecurityFrameworkUtils.getLoginUserId())) {
            return CommonResult.error(400, "不能删除当前登录用户");
        }
        userService.deleteUser(id);
        return CommonResult.success(true);
    }

    @PutMapping("/update-password")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUserPassword(@Valid @RequestBody UserUpdatePasswordReqVO reqVO) {
        userService.updateUserPassword(reqVO.id(), reqVO.password());
        return CommonResult.success(true);
    }

    @PutMapping("/assign-role")
    @PreAuthorize("@ss.hasPermission('system:user:assign-role')")
    public CommonResult<Boolean> assignUserRole(@Valid @RequestBody UserAssignRoleReqVO reqVO) {
        userService.assignUserRole(reqVO.id(), reqVO.roleIds());
        return CommonResult.success(true);
    }

    @GetMapping("/get-role-ids")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<List<Long>> getUserRoleIds(@RequestParam("id") Long id) {
        return CommonResult.success(userService.getUserRoleIds(id));
    }

    private UserRespVO toRespVO(SystemUsersDO user) {
        UserRespVO vo = new UserRespVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setDeptId(user.getDeptId());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    public record UserCreateReqVO(@NotBlank(message = "用户名不能为空") String username,
                                  @NotBlank(message = "密码不能为空") String password,
                                  @NotBlank(message = "昵称不能为空") String nickname,
                                  Long deptId) {
    }

    public record UserUpdateReqVO(@NotNull(message = "用户编号不能为空") Long id,
                                  String nickname, Long deptId, Integer status) {
    }

    public record UserUpdatePasswordReqVO(@NotNull(message = "用户编号不能为空") Long id,
                                          @NotBlank(message = "密码不能为空") String password) {
    }

    public record UserAssignRoleReqVO(@NotNull(message = "用户编号不能为空") Long id,
                                      List<Long> roleIds) {
    }

    @lombok.Data
    public static class UserRespVO {
        private Long id;
        private String username;
        private String nickname;
        private Long deptId;
        private Integer status;
        private java.time.LocalDateTime createTime;
    }
}
