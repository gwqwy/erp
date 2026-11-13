package cn.tiangong.module.system.service.auth;

import cn.tiangong.module.system.dal.dataobject.SystemMenuDO;
import cn.tiangong.module.system.dal.dataobject.SystemUsersDO;

import java.util.List;
import java.util.Set;

/**
 * 认证服务：登录 / 登出 / 权限信息。
 */
public interface AdminAuthService {

    /** 登录成功返回用户编号与 accessToken；失败抛业务异常 */
    LoginResult login(String username, String password);

    /** 登出：作废 Token */
    void logout(String token);

    /** 当前用户的角色、权限与可见菜单 */
    PermissionInfo getPermissionInfo(Long userId);

    record LoginResult(Long userId, String accessToken) {
    }

    record PermissionInfo(SystemUsersDO user, Set<String> roles, Set<String> permissions,
                          List<SystemMenuDO> menus) {
    }
}
