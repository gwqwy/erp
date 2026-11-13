package cn.tiangong.module.bas.service;

import cn.tiangong.module.bas.dal.dataobject.ErpWarehouseDO;
import cn.tiangong.module.bas.dal.mysql.ErpWarehouseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final ErpWarehouseMapper warehouseMapper;

    @Override
    public List<ErpWarehouseDO> getWarehouseList(Integer status) {
        return warehouseMapper.selectList(new LambdaQueryWrapper<ErpWarehouseDO>()
                .eq(status != null, ErpWarehouseDO::getStatus, status)
                .orderByAsc(ErpWarehouseDO::getId));
    }

    @Override
    public ErpWarehouseDO getWarehouse(Long id) {
        ErpWarehouseDO warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw of(2_005, 1, "仓库不存在");
        }
        return warehouse;
    }

    @Override
    public Long createWarehouse(ErpWarehouseDO warehouse) {
        if (warehouse.getStatus() == null) {
            warehouse.setStatus(0);
        }
        warehouseMapper.insert(warehouse);
        return warehouse.getId();
    }

    @Override
    public void updateWarehouse(ErpWarehouseDO warehouse) {
        getWarehouse(warehouse.getId());
        warehouseMapper.updateById(warehouse);
    }

    @Override
    public void deleteWarehouse(Long id) {
        getWarehouse(id);
        warehouseMapper.deleteById(id);
    }
}
