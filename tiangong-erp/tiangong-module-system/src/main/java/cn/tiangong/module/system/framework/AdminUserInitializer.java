package cn.tiangong.module.system.framework;

import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.module.system.dal.dataobject.SystemUserRoleDO;
import cn.tiangong.module.system.dal.mysql.SystemUserRoleMapper;
import cn.tiangong.module.system.service.user.UserService;
import cn.tiangong.module.system.service.role.RoleServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 初始管理员：首次启动时若无 admin 用户则创建 admin/admin123 并绑定超管角色。
 * 生产环境首次登录后请立即修改密码。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AdminUserInitializer implements ApplicationRunner {

    private final UserService userService;
    private final SystemUserRoleMapper userRoleMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Long userId = userService.ensureAdminUser("admin", "admin123", "超级管理员");
            Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SystemUserRoleDO>()
                    .eq(SystemUserRoleDO::getUserId, userId)
                    .eq(SystemUserRoleDO::getRoleId, RoleServiceImpl.SUPER_ADMIN_ID));
            if (count == 0) {
                SystemUserRoleDO userRole = new SystemUserRoleDO();
                userRole.setUserId(userId);
                userRole.setRoleId(RoleServiceImpl.SUPER_ADMIN_ID);
                userRoleMapper.insert(userRole);
            }
            log.info("[AdminUserInitializer] 初始管理员就绪: userId={}", userId);
        } catch (ServiceException e) {
            log.warn("[AdminUserInitializer] 初始管理员创建跳过: {}", e.getMessage());
        }
    }
}
