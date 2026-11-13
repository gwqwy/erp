package cn.tiangong.module.system.controller.admin.auth;

import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.system.dal.dataobject.SystemMenuDO;
import cn.tiangong.module.system.service.auth.AdminAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/system/auth")
public class AuthController {

    private final AdminAuthService authService;

    @PostMapping("/login")
    public CommonResult<AuthLoginRespVO> login(@Valid @RequestBody AuthLoginReqVO reqVO) {
        AdminAuthService.LoginResult result = authService.login(reqVO.getUsername(), reqVO.getPassword());
        return CommonResult.success(new AuthLoginRespVO(result.userId(), result.accessToken()));
    }

    @PostMapping("/logout")
    public CommonResult<Boolean> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String token) {
            authService.logout(token);
        }
        return CommonResult.success(true);
    }

    @GetMapping("/get-permission-info")
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        AdminAuthService.PermissionInfo info = authService.getPermissionInfo(userId);
        AuthPermissionInfoRespVO respVO = new AuthPermissionInfoRespVO();
        respVO.setUser(new UserVO(info.user().getId(), info.user().getUsername(),
                info.user().getNickname(), info.user().getDeptId()));
        respVO.setRoles(info.roles());
        respVO.setPermissions(info.permissions());
        respVO.setMenus(buildMenuTree(info.menus(), 0L));
        return CommonResult.success(respVO);
    }

    /** parentId 从 0 开始构建菜单树 */
    private List<MenuVO> buildMenuTree(List<SystemMenuDO> menus, Long parentId) {
        Map<Long, List<SystemMenuDO>> byParent = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return buildChildren(byParent, parentId);
    }

    private List<MenuVO> buildChildren(Map<Long, List<SystemMenuDO>> byParent, Long parentId) {
        List<SystemMenuDO> children = byParent.getOrDefault(parentId, new ArrayList<>());
        return children.stream()
                .sorted(Comparator.comparing(m -> m.getSort() == null ? 0 : m.getSort()))
                .map(m -> new MenuVO(m.getId(), m.getParentId(), m.getName(),
                        m.getPath(), m.getComponent(), m.getType(), m.getSort(),
                        buildChildren(byParent, m.getId())))
                .toList();
    }

    @Data
    public static class AuthLoginReqVO {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    public record AuthLoginRespVO(Long userId, String accessToken) {
    }

    public record UserVO(Long id, String username, String nickname, Long deptId) {
    }

    public record MenuVO(Long id, Long parentId, String name, String path, String component,
                         Integer type, Integer sort, List<MenuVO> children) {
    }

    @lombok.Data
    public static class AuthPermissionInfoRespVO {
        private UserVO user;
        private java.util.Set<String> roles;
        private java.util.Set<String> permissions;
        private List<MenuVO> menus;
    }
}
