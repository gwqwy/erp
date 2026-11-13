package cn.tiangong.module.bas.service;

import cn.tiangong.module.bas.dal.dataobject.ErpUnitDO;

import java.util.List;

public interface UnitService {

    List<ErpUnitDO> getUnitList(Integer status);

    Long createUnit(String name);

    void updateUnit(Long id, String name, Integer status);

    void deleteUnit(Long id);
}
