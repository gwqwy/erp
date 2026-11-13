package cn.tiangong.module.system.service.user;

import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemUserRoleDO;
import cn.tiangong.module.system.dal.dataobject.SystemUsersDO;
import cn.tiangong.module.system.dal.mysql.SystemUserRoleMapper;
import cn.tiangong.module.system.dal.mysql.SystemUsersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final SystemUsersMapper userMapper;
    private final SystemUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<SystemUsersDO> getUserPage(PageParam pageParam, String username, String nickname) {
        LambdaQueryWrapper<SystemUsersDO> wrapper = new LambdaQueryWrapper<SystemUsersDO>()
                .like(username != null && !username.isBlank(), SystemUsersDO::getUsername, username)
                .like(nickname != null && !nickname.isBlank(), SystemUsersDO::getNickname, nickname)
                .orderByDesc(SystemUsersDO::getId);
        return userMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SystemUsersDO getUser(Long id) {
        SystemUsersDO user = userMapper.selectById(id);
        if (user == null) {
            throw of(1_004, 1, "用户不存在");
        }
        return user;
    }

    @Override
    public SystemUsersDO selectByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public Long createUser(String username, String password, String nickname, Long deptId) {
        if (userMapper.selectByUsername(username) != null) {
            throw of(1_004, 2, "用户名已存在");
        }
        SystemUsersDO user = new SystemUsersDO();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setDeptId(deptId);
        user.setStatus(0);
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public void updateUser(Long id, String nickname, Long deptId, Integer status) {
        SystemUsersDO user = getUser(id);
        user.setNickname(nickname != null ? nickname : user.getNickname());
        user.setDeptId(deptId != null ? deptId : user.getDeptId());
        user.setStatus(status != null ? status : user.getStatus());
        userMapper.updateById(user);
    }

    @Override
    public void updateUserPassword(Long id, String newPassword) {
        SystemUsersDO user = getUser(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SystemUserRoleDO>()
                .eq(SystemUserRoleDO::getUserId, id));
    }

    @Override
    public List<SystemUsersDO> getUserListByDept(Long deptId) {
        return userMapper.selectList(new LambdaQueryWrapper<SystemUsersDO>()
                .eq(SystemUsersDO::getDeptId, deptId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRole(Long userId, List<Long> roleIds) {
        getUser(userId);
        userRoleMapper.delete(new LambdaQueryWrapper<SystemUserRoleDO>()
                .eq(SystemUserRoleDO::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            SystemUserRoleDO ur = new SystemUserRoleDO();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        List<SystemUserRoleDO> list = userRoleMapper.selectList(
                new LambdaQueryWrapper<SystemUserRoleDO>().eq(SystemUserRoleDO::getUserId, userId));
        return list.isEmpty() ? Collections.emptyList()
                : list.stream().map(SystemUserRoleDO::getRoleId).toList();
    }

    /** 供初始化器调用：若系统尚无 admin 用户则创建（角色绑定由调用方处理） */
    public Long ensureAdminUser(String username, String rawPassword, String nickname) {
        SystemUsersDO existing = userMapper.selectByUsername(username);
        if (existing != null) {
            return existing.getId();
        }
        return createUser(username, rawPassword, nickname, 1L);
    }
}
