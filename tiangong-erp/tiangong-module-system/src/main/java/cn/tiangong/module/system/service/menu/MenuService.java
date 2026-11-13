package cn.tiangong.module.system.service.menu;

import cn.tiangong.module.system.dal.dataobject.SystemMenuDO;

import java.util.List;

public interface MenuService {

    List<SystemMenuDO> getMenuList();

    SystemMenuDO getMenu(Long id);

    Long createMenu(SystemMenuDO menu);

    void updateMenu(SystemMenuDO menu);

    void deleteMenu(Long id);
}
