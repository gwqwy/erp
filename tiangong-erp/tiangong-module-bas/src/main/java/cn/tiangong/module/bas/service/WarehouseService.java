package cn.tiangong.module.bas.service;

import cn.tiangong.module.bas.dal.dataobject.ErpWarehouseDO;

import java.util.List;

public interface WarehouseService {

    List<ErpWarehouseDO> getWarehouseList(Integer status);

    ErpWarehouseDO getWarehouse(Long id);

    Long createWarehouse(ErpWarehouseDO warehouse);

    void updateWarehouse(ErpWarehouseDO warehouse);

    void deleteWarehouse(Long id);
}
