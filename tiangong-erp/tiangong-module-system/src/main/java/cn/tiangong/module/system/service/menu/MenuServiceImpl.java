package cn.tiangong.module.system.service.menu;

import cn.tiangong.module.system.dal.dataobject.SystemMenuDO;
import cn.tiangong.module.system.dal.dataobject.SystemRoleMenuDO;
import cn.tiangong.module.system.dal.mysql.SystemMenuMapper;
import cn.tiangong.module.system.dal.mysql.SystemRoleMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class MenuServiceImpl implements MenuService {

    private final SystemMenuMapper menuMapper;
    private final SystemRoleMenuMapper roleMenuMapper;

    @Override
    public List<SystemMenuDO> getMenuList() {
        return menuMapper.selectList(new LambdaQueryWrapper<SystemMenuDO>()
                .orderByAsc(SystemMenuDO::getSort));
    }

    @Override
    public SystemMenuDO getMenu(Long id) {
        SystemMenuDO menu = menuMapper.selectById(id);
        if (menu == null) {
            throw of(1_005, 1, "菜单不存在");
        }
        return menu;
    }

    @Override
    public Long createMenu(SystemMenuDO menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(0);
        }
        if (menu.getType() == null) {
            menu.setType(2);
        }
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    public void updateMenu(SystemMenuDO menu) {
        getMenu(menu.getId());
        menuMapper.updateById(menu);
    }

    @Override
    public void deleteMenu(Long id) {
        getMenu(id);
        if (menuMapper.selectCount(SystemMenuDO::getParentId, id) > 0) {
            throw of(1_005, 2, "存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SystemRoleMenuDO>()
                .eq(SystemRoleMenuDO::getMenuId, id));
    }
}
