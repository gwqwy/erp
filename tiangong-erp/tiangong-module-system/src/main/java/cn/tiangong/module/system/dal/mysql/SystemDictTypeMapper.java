package cn.tiangong.module.system.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.system.dal.dataobject.SystemDictTypeDO;

public interface SystemDictTypeMapper extends BaseMapperX<SystemDictTypeDO> {

    default SystemDictTypeDO selectByType(String type) {
        return selectOne(SystemDictTypeDO::getType, type);
    }
}
