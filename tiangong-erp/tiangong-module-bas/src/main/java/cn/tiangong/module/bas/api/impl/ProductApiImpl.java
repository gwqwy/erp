package cn.tiangong.module.bas.api.impl;

import cn.tiangong.module.bas.dal.dataobject.ErpProductDO;
import cn.tiangong.module.bas.dal.mysql.ErpProductMapper;
import cn.tiangong.module.bas.api.ProductApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductApiImpl implements ProductApi {

    private final ErpProductMapper productMapper;

    @Override
    public Map<Long, String> getProductNames(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(ErpProductDO::getId, ErpProductDO::getName, (a, b) -> a));
    }

    @Override
    public String getProductName(Long productId) {
        return getProductNames(Collections.singletonList(productId))
                .getOrDefault(productId, "未知产品");
    }

    @Override
    public boolean isBatchEnabled(Long productId) {
        ErpProductDO product = productMapper.selectById(productId);
        return product != null && Boolean.TRUE.equals(product.getIsBatch());
    }

    @Override
    public java.util.List<StockLimit> getStockLimits() {
        return productMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpProductDO>()
                .eq(ErpProductDO::getStatus, 0)
                .and(w -> w.isNotNull(ErpProductDO::getStockMin).or().isNotNull(ErpProductDO::getStockMax)))
                .stream()
                .map(p -> new StockLimit(p.getId(), p.getName(), p.getStockMin(), p.getStockMax()))
                .toList();
    }
}
