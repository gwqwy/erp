package cn.tiangong.module.system.service.role;

import cn.tiangong.framework.web.core.enums.CommonStatusEnum;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemMenuDO;
import cn.tiangong.module.system.dal.dataobject.SystemRoleDO;
import cn.tiangong.module.system.dal.dataobject.SystemRoleMenuDO;
import cn.tiangong.module.system.dal.dataobject.SystemUserRoleDO;
import cn.tiangong.module.system.dal.mysql.SystemMenuMapper;
import cn.tiangong.module.system.dal.mysql.SystemRoleMapper;
import cn.tiangong.module.system.dal.mysql.SystemRoleMenuMapper;
import cn.tiangong.module.system.dal.mysql.SystemUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class RoleServiceImpl implements RoleService {

    public static final String SUPER_ADMIN_CODE = "super_admin";
    public static final Long SUPER_ADMIN_ID = 1L;
    public static final String WILDCARD_PERMISSION = "*:*:*";

    private final SystemRoleMapper roleMapper;
    private final SystemUserRoleMapper userRoleMapper;
    private final SystemRoleMenuMapper roleMenuMapper;
    private final SystemMenuMapper menuMapper;

    @Override
    public PageResult<SystemRoleDO> getRolePage(PageParam pageParam, String name) {
        LambdaQueryWrapper<SystemRoleDO> wrapper = new LambdaQueryWrapper<SystemRoleDO>()
                .like(name != null && !name.isBlank(), SystemRoleDO::getName, name)
                .orderByAsc(SystemRoleDO::getSort);
        return roleMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SystemRoleDO getRole(Long id) {
        SystemRoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw of(1_003, 1, "角色不存在");
        }
        return role;
    }

    @Override
    public Long createRole(String name, String code, Integer sort, String remark) {
        if (roleMapper.selectCount(SystemRoleDO::getCode, code) > 0) {
            throw of(1_003, 2, "角色编码已存在");
        }
        SystemRoleDO role = new SystemRoleDO();
        role.setName(name);
        role.setCode(code);
        role.setSort(sort);
        role.setRemark(remark);
        role.setStatus(CommonStatusEnum.ENABLE.getValue());
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    public void updateRole(Long id, String name, String code, Integer sort, String remark, Integer status) {
        SystemRoleDO role = getRole(id);
        if (SUPER_ADMIN_ID.equals(id) && CommonStatusEnum.DISABLE.getValue().equals(status)) {
            throw of(1_003, 3, "内置超管角色不允许停用");
        }
        role.setName(name != null ? name : role.getName());
        role.setCode(code != null ? code : role.getCode());
        role.setSort(sort != null ? sort : role.getSort());
        role.setRemark(remark != null ? remark : role.getRemark());
        role.setStatus(status != null ? status : role.getStatus());
        roleMapper.updateById(role);
    }

    @Override
    public void deleteRole(Long id) {
        if (SUPER_ADMIN_ID.equals(id)) {
            throw of(1_003, 3, "内置超管角色不允许删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SystemRoleMenuDO>()
                .eq(SystemRoleMenuDO::getRoleId, id));
    }

    @Override
    public Set<String> getUserRoleCodes(Long userId) {
        return getUserRoles(userId).stream().map(SystemRoleDO::getCode).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getUserPermissions(Long userId) {
        List<SystemRoleDO> roles = getUserRoles(userId);
        if (roles.isEmpty()) {
            return Set.of();
        }
        if (roles.stream().anyMatch(r -> SUPER_ADMIN_CODE.equals(r.getCode()))) {
            return Set.of(WILDCARD_PERMISSION);
        }
        List<Long> menuIds = selectMenuIdsByRoles(roles);
        if (menuIds.isEmpty()) {
            return Set.of();
        }
        return menuMapper.selectBatchIds(menuIds).stream()
                .filter(m -> m.getPermission() != null && !m.getPermission().isBlank())
                .filter(m -> CommonStatusEnum.ENABLE.getValue().equals(m.getStatus()))
                .map(SystemMenuDO::getPermission)
                .collect(Collectors.toSet());
    }

    @Override
    public List<SystemMenuDO> getDisplayMenus(Long userId, Set<String> permissions) {
        List<Long> menuIds;
        if (permissions.contains(WILDCARD_PERMISSION)) {
            return menuMapper.selectList(new LambdaQueryWrapper<SystemMenuDO>()
                    .in(SystemMenuDO::getType, List.of(1, 2))
                    .eq(SystemMenuDO::getStatus, CommonStatusEnum.ENABLE.getValue())
                    .orderByAsc(SystemMenuDO::getSort));
        }
        List<SystemRoleDO> roles = getUserRoles(userId);
        menuIds = selectMenuIdsByRoles(roles);
        return menuIds.isEmpty() ? Collections.emptyList()
                : menuMapper.selectBatchIds(menuIds).stream()
                .filter(m -> List.of(1, 2).contains(m.getType()))
                .filter(m -> CommonStatusEnum.ENABLE.getValue().equals(m.getStatus()))
                .sorted(Comparator.comparing(SystemMenuDO::getSort))
                .toList();
    }

    @Override
    public List<SystemRoleDO> getRoleList() {
        return roleMapper.selectList(new LambdaQueryWrapper<SystemRoleDO>()
                .orderByAsc(SystemRoleDO::getSort));
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SystemRoleMenuDO>()
                        .eq(SystemRoleMenuDO::getRoleId, roleId))
                .stream().map(SystemRoleMenuDO::getMenuId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenu(Long roleId, List<Long> menuIds) {
        getRole(roleId);
        roleMenuMapper.delete(new LambdaQueryWrapper<SystemRoleMenuDO>()
                .eq(SystemRoleMenuDO::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds) {
            SystemRoleMenuDO rm = new SystemRoleMenuDO();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    private List<SystemRoleDO> getUserRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SystemUserRoleDO>()
                        .eq(SystemUserRoleDO::getUserId, userId))
                .stream().map(SystemUserRoleDO::getRoleId).toList();
        return roleIds.isEmpty() ? Collections.emptyList()
                : roleMapper.selectBatchIds(roleIds).stream()
                .filter(r -> CommonStatusEnum.ENABLE.getValue().equals(r.getStatus()))
                .toList();
    }

    private List<Long> selectMenuIdsByRoles(List<SystemRoleDO> roles) {
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roles.stream().map(SystemRoleDO::getId).toList();
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SystemRoleMenuDO>()
                        .in(SystemRoleMenuDO::getRoleId, roleIds))
                .stream().map(SystemRoleMenuDO::getMenuId).distinct().toList();
    }
}
