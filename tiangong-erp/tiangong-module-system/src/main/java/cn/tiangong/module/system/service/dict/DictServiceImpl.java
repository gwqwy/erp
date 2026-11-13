package cn.tiangong.module.system.service.dict;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemDictDataDO;
import cn.tiangong.module.system.dal.dataobject.SystemDictTypeDO;
import cn.tiangong.module.system.dal.mysql.SystemDictDataMapper;
import cn.tiangong.module.system.dal.mysql.SystemDictTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class DictServiceImpl implements DictService {

    private final SystemDictTypeMapper dictTypeMapper;
    private final SystemDictDataMapper dictDataMapper;

    @Override
    public PageResult<SystemDictTypeDO> getDictTypePage(PageParam pageParam, String name) {
        LambdaQueryWrapper<SystemDictTypeDO> wrapper = new LambdaQueryWrapper<SystemDictTypeDO>()
                .like(name != null && !name.isBlank(), SystemDictTypeDO::getName, name)
                .orderByAsc(SystemDictTypeDO::getType);
        return dictTypeMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Long createDictType(String name, String type) {
        if (dictTypeMapper.selectByType(type) != null) {
            throw of(1_007, 1, "字典类型已存在");
        }
        SystemDictTypeDO dictType = new SystemDictTypeDO();
        dictType.setName(name);
        dictType.setType(type);
        dictType.setStatus(0);
        dictTypeMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public void updateDictType(Long id, String name) {
        SystemDictTypeDO dictType = dictTypeMapper.selectById(id);
        if (dictType == null) {
            throw of(1_007, 2, "字典类型不存在");
        }
        dictType.setName(name != null ? name : dictType.getName());
        dictTypeMapper.updateById(dictType);
    }

    @Override
    public void deleteDictType(Long id) {
        SystemDictTypeDO dictType = dictTypeMapper.selectById(id);
        if (dictType == null) {
            throw of(1_007, 2, "字典类型不存在");
        }
        Long dataCount = dictDataMapper.selectCount(SystemDictDataDO::getDictType, dictType.getType());
        if (dataCount > 0) {
            throw of(1_007, 3, "该类型下存在字典数据，无法删除");
        }
        dictTypeMapper.deleteById(id);
    }

    @Override
    public List<SystemDictDataDO> getDictDataList(String dictType) {
        return dictDataMapper.selectList(new LambdaQueryWrapper<SystemDictDataDO>()
                .eq(SystemDictDataDO::getDictType, dictType)
                .orderByAsc(SystemDictDataDO::getSort));
    }

    @Override
    public Long createDictData(String dictType, String label, String value, Integer sort) {
        SystemDictDataDO data = new SystemDictDataDO();
        data.setDictType(dictType);
        data.setLabel(label);
        data.setValue(value);
        data.setSort(sort != null ? sort : 0);
        data.setStatus(0);
        dictDataMapper.insert(data);
        return data.getId();
    }

    @Override
    public void updateDictData(Long id, String label, String value, Integer sort) {
        SystemDictDataDO data = dictDataMapper.selectById(id);
        if (data == null) {
            throw of(1_007, 4, "字典数据不存在");
        }
        data.setLabel(label != null ? label : data.getLabel());
        data.setValue(value != null ? value : data.getValue());
        data.setSort(sort != null ? sort : data.getSort());
        dictDataMapper.updateById(data);
    }

    @Override
    public void deleteDictData(Long id) {
        if (dictDataMapper.selectById(id) == null) {
            throw of(1_007, 4, "字典数据不存在");
        }
        dictDataMapper.deleteById(id);
    }
}
