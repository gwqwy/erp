package cn.tiangong.module.system.service.auth;

import cn.tiangong.framework.security.core.LoginUser;
import cn.tiangong.framework.security.core.TokenService;
import cn.tiangong.framework.web.core.enums.CommonStatusEnum;
import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.module.system.dal.dataobject.SystemMenuDO;
import cn.tiangong.module.system.dal.dataobject.SystemUsersDO;
import cn.tiangong.module.system.dal.mysql.SystemUsersMapper;
import cn.tiangong.module.system.service.role.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    public static final String SUPER_ADMIN = "super_admin";
    public static final String WILDCARD = "*:*:*";

    private final SystemUsersMapper userMapper;
    private final RoleService roleService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResult login(String username, String password) {
        SystemUsersDO user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw of(1_002, 1, "登录失败，用户名或密码不正确");
        }
        if (CommonStatusEnum.DISABLE.getValue().equals(user.getStatus())) {
            throw of(1_002, 2, "账号已停用");
        }
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getNickname(),
                user.getDeptId(), 1L, roleService.getUserPermissions(user.getId()));
        String token = tokenService.createToken(loginUser);
        log.info("[login] 用户登录成功: id={}, username={}", user.getId(), username);
        return new LoginResult(user.getId(), token);
    }

    @Override
    public void logout(String token) {
        tokenService.deleteToken(token);
    }

    @Override
    public PermissionInfo getPermissionInfo(Long userId) {
        SystemUsersDO user = userMapper.selectById(userId);
        if (user == null) {
            throw of(1_002, 3, "用户不存在");
        }
        user.setPassword(null);
        Set<String> permissions = roleService.getUserPermissions(userId);
        List<SystemMenuDO> menus = roleService.getDisplayMenus(userId, permissions);
        Set<String> roles = roleService.getUserRoleCodes(userId);
        return new PermissionInfo(user, roles, permissions, menus);
    }
}
