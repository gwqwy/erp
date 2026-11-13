package cn.tiangong.module.bas.api;

import java.util.Collection;
import java.util.Map;

/**
 * 基础数据 - 仓库对外 API。
 */
public interface WarehouseApi {

    Map<Long, String> getWarehouseNames(Collection<Long> warehouseIds);

    String getWarehouseName(Long warehouseId);
}
