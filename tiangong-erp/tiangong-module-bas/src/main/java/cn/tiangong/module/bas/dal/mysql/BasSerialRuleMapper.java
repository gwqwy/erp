package cn.tiangong.module.bas.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.bas.dal.dataobject.BasSerialRuleDO;

public interface BasSerialRuleMapper extends BaseMapperX<BasSerialRuleDO> {

    default BasSerialRuleDO selectByBizType(String bizType) {
        return selectOne(BasSerialRuleDO::getBizType, bizType);
    }
}
