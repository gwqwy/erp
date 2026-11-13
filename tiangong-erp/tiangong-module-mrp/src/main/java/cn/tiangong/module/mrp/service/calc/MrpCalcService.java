package cn.tiangong.module.mrp.service.calc;

import cn.tiangong.module.bas.api.ProductApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomItemDO;
import cn.tiangong.module.mrp.service.bom.BomService;
import cn.tiangong.module.stk.api.StockQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MRP 简化试算：按需求逐层展开 BOM，每层先扣减现有库存（跨层共享库存消耗），
 * 产出叶子原材料的净需求。结果不落库（正式计划落库留 M4）。
 */
@RequiredArgsConstructor
@Service
public class MrpCalcService {

    private final BomService bomService;
    private final StockQueryApi stockQueryApi;
    private final ProductApi productApi;

    public CalcResult calc(Long warehouseId, List<Demand> demands) {
        if (demands == null || demands.isEmpty()) {
            throw cn.tiangong.framework.web.core.exception.ServiceException.of(9_03, 1, "需求行不能为空");
        }
        // 仓库现有库存快照（跨层共享消耗）
        Set<Long> involved = new HashSet<>();
        for (Demand d : demands) {
            collectProducts(d.productId(), involved);
        }
        Map<Long, BigDecimal> stock = stockQueryApi.getQuantQty(warehouseId, involved);
        Map<Long, BigDecimal> remaining = new HashMap<>(stock);

        Map<Long, BigDecimal> netDemand = new HashMap<>();
        for (Demand d : demands) {
            expandNet(d.productId(), d.qty(), 0, remaining, netDemand);
        }

        Map<Long, String> names = productApi.getProductNames(netDemand.keySet());
        List<CalcLine> lines = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : netDemand.entrySet()) {
            lines.add(new CalcLine(entry.getKey(),
                    names.getOrDefault(entry.getKey(), "未知产品"),
                    stock.getOrDefault(entry.getKey(), BigDecimal.ZERO),
                    entry.getValue()));
        }
        lines.sort((a, b) -> a.productName().compareTo(b.productName()));
        return new CalcResult(lines);
    }

    private void expandNet(Long productId, BigDecimal grossQty, int depth,
                           Map<Long, BigDecimal> remaining, Map<Long, BigDecimal> netDemand) {
        if (depth > 10) {
            throw cn.tiangong.framework.web.core.exception.ServiceException.of(9_03, 2, "BOM 展开超过最大层级");
        }
        BigDecimal available = remaining.getOrDefault(productId, BigDecimal.ZERO);
        BigDecimal net = grossQty.subtract(available).max(BigDecimal.ZERO);
        // 消耗可用库存（含跨层）
        BigDecimal consumed = grossQty.min(available);
        remaining.put(productId, available.subtract(consumed));
        if (net.signum() <= 0) {
            return;
        }
        ErpMrpBomDO bom = bomService.getDefaultBom(productId);
        if (bom == null || bom.getQty().signum() <= 0) {
            netDemand.merge(productId, net, BigDecimal::add);
            return;
        }
        for (ErpMrpBomItemDO item : bomService.getBomDetail(bom.getId()).items()) {
            BigDecimal componentGross = item.getQty().multiply(net)
                    .divide(bom.getQty(), 6, RoundingMode.HALF_UP);
            BigDecimal lossRate = item.getLossRate() == null ? BigDecimal.ZERO : item.getLossRate();
            BigDecimal withLoss = componentGross.multiply(
                    BigDecimal.ONE.add(lossRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)));
            expandNet(item.getProductId(), withLoss, depth + 1, remaining, netDemand);
        }
    }

    private void collectProducts(Long productId, Set<Long> out) {
        if (!out.add(productId)) {
            return;
        }
        ErpMrpBomDO bom = bomService.getDefaultBom(productId);
        if (bom != null) {
            for (ErpMrpBomItemDO item : bomService.getBomDetail(bom.getId()).items()) {
                collectProducts(item.getProductId(), out);
            }
        }
    }

    public record Demand(Long productId, BigDecimal qty) {
    }

    public record CalcLine(Long productId, String productName, BigDecimal stockQty, BigDecimal netQty) {
    }

    public record CalcResult(List<CalcLine> lines) {
    }
}
