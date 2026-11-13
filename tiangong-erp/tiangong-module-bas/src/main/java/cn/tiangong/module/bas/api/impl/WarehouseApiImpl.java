package cn.tiangong.module.bas.api.impl;

import cn.tiangong.module.bas.dal.dataobject.ErpWarehouseDO;
import cn.tiangong.module.bas.dal.mysql.ErpWarehouseMapper;
import cn.tiangong.module.bas.api.WarehouseApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class WarehouseApiImpl implements WarehouseApi {

    private final ErpWarehouseMapper warehouseMapper;

    @Override
    public Map<Long, String> getWarehouseNames(Collection<Long> warehouseIds) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(ErpWarehouseDO::getId, ErpWarehouseDO::getName, (a, b) -> a));
    }

    @Override
    public String getWarehouseName(Long warehouseId) {
        return getWarehouseNames(Collections.singletonList(warehouseId))
                .getOrDefault(warehouseId, "未知仓库");
    }
}
