package cn.tiangong.module.system.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.system.dal.dataobject.SystemUsersDO;

public interface SystemUsersMapper extends BaseMapperX<SystemUsersDO> {

    default SystemUsersDO selectByUsername(String username) {
        return selectOne(SystemUsersDO::getUsername, username);
    }
}
