package cn.tiangong.module.stk.api.impl;

import cn.tiangong.module.stk.dal.dataobject.StockMoveDO;
import cn.tiangong.module.stk.dal.dataobject.StockQuantDO;
import cn.tiangong.module.stk.dal.mysql.StockMoveMapper;
import cn.tiangong.module.stk.dal.mysql.StockQuantMapper;
import cn.tiangong.module.stk.api.StockQueryApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class StockQueryApiImpl implements StockQueryApi {

    private final StockMoveMapper moveMapper;
    private final StockQuantMapper quantMapper;

    @Override
    public BigDecimal sumMoveAmount(String bizType, Collection<Long> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return moveMapper.selectList(new LambdaQueryWrapper<StockMoveDO>()
                        .eq(StockMoveDO::getBizType, bizType)
                        .in(StockMoveDO::getBizId, bizIds))
                .stream()
                .map(m -> m.getAmount() == null ? BigDecimal.ZERO : m.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<Long, BigDecimal> getQuantQty(Long warehouseId, Collection<Long> productIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (warehouseId == null || productIds == null || productIds.isEmpty()) {
            return result;
        }
        for (StockQuantDO quant : quantMapper.selectList(new LambdaQueryWrapper<StockQuantDO>()
                .eq(StockQuantDO::getWarehouseId, warehouseId)
                .in(StockQuantDO::getProductId, productIds))) {
            result.merge(quant.getProductId(), quant.getQty(), BigDecimal::add);
        }
        return result;
    }

    @Override
    public BigDecimal totalStockValue() {
        return quantMapper.selectList(new LambdaQueryWrapper<StockQuantDO>()
                        .gt(StockQuantDO::getQty, BigDecimal.ZERO))
                .stream()
                .map(q -> q.getQty().multiply(q.getAvgCost() == null ? BigDecimal.ZERO : q.getAvgCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
