package cn.tiangong.module.system.service.user;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemUsersDO;

import java.util.List;

public interface UserService {

    PageResult<SystemUsersDO> getUserPage(PageParam pageParam, String username, String nickname);

    SystemUsersDO getUser(Long id);

    SystemUsersDO selectByUsername(String username);

    /** 创建用户（密码明文入参，内部加密存储） */
    Long createUser(String username, String password, String nickname, Long deptId);

    void updateUser(Long id, String nickname, Long deptId, Integer status);

    void updateUserPassword(Long id, String newPassword);

    void deleteUser(Long id);

    List<SystemUsersDO> getUserListByDept(Long deptId);

    void assignUserRole(Long userId, List<Long> roleIds);

    List<Long> getUserRoleIds(Long userId);

    /** 若系统尚无该用户名则创建并返回编号，已存在则直接返回其编号（初始化器用） */
    Long ensureAdminUser(String username, String rawPassword, String nickname);
}
