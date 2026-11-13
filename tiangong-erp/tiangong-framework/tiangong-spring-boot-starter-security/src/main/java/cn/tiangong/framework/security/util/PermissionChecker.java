package cn.tiangong.framework.security.util;

import cn.tiangong.framework.security.core.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * 方法级权限检查器，注册为 Bean 名 "ss"。
 * 用法：@PreAuthorize("@ss.hasPermission('sal:order:audit')")
 * 通配：权限集含 "*:*:*"（超级管理员）时放行一切。
 */
public class PermissionChecker {

    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return false;
        }
        Set<String> permissions = loginUser.getPermissions();
        return permissions != null && (permissions.contains(permission) || permissions.contains("*:*:*"));
    }
}
