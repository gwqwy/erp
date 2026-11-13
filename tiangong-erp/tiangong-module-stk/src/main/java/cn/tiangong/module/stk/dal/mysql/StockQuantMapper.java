package cn.tiangong.module.stk.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.stk.dal.dataobject.StockQuantDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

public interface StockQuantMapper extends BaseMapperX<StockQuantDO> {

    default StockQuantDO selectByDim(Long companyId, Long warehouseId, Long locationId,
                                     Long productId, String batchNo) {
        return selectOne(new LambdaQueryWrapper<StockQuantDO>()
                .eq(StockQuantDO::getCompanyId, companyId)
                .eq(StockQuantDO::getWarehouseId, warehouseId)
                .eq(StockQuantDO::getLocationId, locationId)
                .eq(StockQuantDO::getProductId, productId)
                .eq(StockQuantDO::getBatchNo, batchNo == null ? "" : batchNo));
    }
}
