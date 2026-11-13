package cn.tiangong.module.system.service.dict;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemDictDataDO;
import cn.tiangong.module.system.dal.dataobject.SystemDictTypeDO;

import java.util.List;

public interface DictService {

    PageResult<SystemDictTypeDO> getDictTypePage(PageParam pageParam, String name);

    Long createDictType(String name, String type);

    void updateDictType(Long id, String name);

    void deleteDictType(Long id);

    List<SystemDictDataDO> getDictDataList(String dictType);

    Long createDictData(String dictType, String label, String value, Integer sort);

    void updateDictData(Long id, String label, String value, Integer sort);

    void deleteDictData(Long id);
}
