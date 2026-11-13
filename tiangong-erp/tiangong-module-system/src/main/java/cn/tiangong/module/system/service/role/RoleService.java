package cn.tiangong.module.system.service.role;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemMenuDO;
import cn.tiangong.module.system.dal.dataobject.SystemRoleDO;

import java.util.List;
import java.util.Set;

public interface RoleService {

    PageResult<SystemRoleDO> getRolePage(PageParam pageParam, String name);

    SystemRoleDO getRole(Long id);

    Long createRole(String name, String code, Integer sort, String remark);

    void updateRole(Long id, String name, String code, Integer sort, String remark, Integer status);

    void deleteRole(Long id);

    /** 用户的角色编码集合 */
    Set<String> getUserRoleCodes(Long userId);

    /** 用户的权限标识集合（超管返回 [*:*:*]） */
    Set<String> getUserPermissions(Long userId);

    /** 用户可见的导航菜单（目录/菜单，剔除按钮） */
    List<SystemMenuDO> getDisplayMenus(Long userId, Set<String> permissions);

    List<SystemRoleDO> getRoleList();

    /** 角色已分配的菜单编号 */
    List<Long> getRoleMenuIds(Long roleId);

    /** 分配角色菜单（全量覆盖） */
    void assignRoleMenu(Long roleId, List<Long> menuIds);
}
