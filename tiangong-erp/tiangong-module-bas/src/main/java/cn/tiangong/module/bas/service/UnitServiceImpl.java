package cn.tiangong.module.bas.service;

import cn.tiangong.module.bas.dal.dataobject.ErpUnitDO;
import cn.tiangong.module.bas.dal.mysql.ErpUnitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class UnitServiceImpl implements UnitService {

    private final ErpUnitMapper unitMapper;

    @Override
    public List<ErpUnitDO> getUnitList(Integer status) {
        return unitMapper.selectList(new LambdaQueryWrapper<ErpUnitDO>()
                .eq(status != null, ErpUnitDO::getStatus, status)
                .orderByAsc(ErpUnitDO::getId));
    }

    @Override
    public Long createUnit(String name) {
        ErpUnitDO unit = new ErpUnitDO();
        unit.setName(name);
        unit.setStatus(0);
        unitMapper.insert(unit);
        return unit.getId();
    }

    @Override
    public void updateUnit(Long id, String name, Integer status) {
        ErpUnitDO unit = unitMapper.selectById(id);
        if (unit == null) {
            throw of(2_003, 1, "计量单位不存在");
        }
        unit.setName(name != null ? name : unit.getName());
        unit.setStatus(status != null ? status : unit.getStatus());
        unitMapper.updateById(unit);
    }

    @Override
    public void deleteUnit(Long id) {
        if (unitMapper.selectById(id) == null) {
            throw of(2_003, 1, "计量单位不存在");
        }
        unitMapper.deleteById(id);
    }
}
