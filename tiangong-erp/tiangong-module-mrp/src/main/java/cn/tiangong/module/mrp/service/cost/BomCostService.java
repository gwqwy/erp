package cn.tiangong.module.mrp.service.cost;

import cn.tiangong.module.bas.api.ProductApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomItemDO;
import cn.tiangong.module.mrp.service.bom.BomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BOM 成本卷算：按默认 BOM 逐层递归，叶子原材料取采购价；
 * 组件自身有 BOM 则卷算其成本，无 BOM 直接取采购价。
 */
@RequiredArgsConstructor
@Service
public class BomCostService {

    private final BomService bomService;
    private final ProductApi productApi;

    /** 卷算一行：productId 的单位成本 = BOM 卷算 或 采购价 */
    public RollupResult rollup(Long productId) {
        Set<Long> visiting = new HashSet<>();
        List<CostLine> lines = new ArrayList<>();
        BigDecimal cost = roll(productId, BigDecimal.ONE, lines, visiting, 0);
        Map<Long, String> names = productApi.getProductNames(List.of(productId));
        return new RollupResult(productId,
                names.getOrDefault(productId, "未知产品"),
                cost, lines);
    }

    /**
     * 递归卷算：qtyPerParent 份该产品的成本。
     * 该产品有默认 BOM：Σ(子件单位用量×子件成本)÷BOM 产量 × qtyPerParent，并展开明细；
     * 无 BOM：采购价 × qtyPerParent，作为叶子行记录。
     */
    private BigDecimal roll(Long productId, BigDecimal qtyPerParent,
                            List<CostLine> lines, Set<Long> visiting, int depth) {
        if (depth > 20 || visiting.contains(productId)) {
            lines.add(new CostLine(productId, null, BigDecimal.ZERO, BigDecimal.ZERO,
                    qtyPerParent, "循环引用/层级过深，按 0 计", depth));
            return BigDecimal.ZERO;
        }
        visiting.add(productId);
        try {
            ErpMrpBomDO bom = bomService.getDefaultBom(productId);
            if (bom == null) {
                BigDecimal price = productApi.getPurchasePrices(java.util.Set.of(productId))
                        .getOrDefault(productId, BigDecimal.ZERO);
                lines.add(new CostLine(productId, null, price, price.multiply(qtyPerParent),
                        qtyPerParent, "原材料", depth));
                return price.multiply(qtyPerParent);
            }
            // 组件行展开
            List<ErpMrpBomItemDO> items = bomService.getBomDetail(bom.getId()).items();
            BigDecimal total = BigDecimal.ZERO;
            Map<Long, String> names = productApi.getProductNames(
                    items.stream().map(ErpMrpBomItemDO::getProductId).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
            List<CostLine> childLines = new ArrayList<>();
            for (ErpMrpBomItemDO item : items) {
                // 单份成品对该子件的用量（含损耗率），再乘本层倍数
                BigDecimal perUnit = item.getQty().multiply(BigDecimal.ONE.add(
                        item.getLossRate() == null ? BigDecimal.ZERO : item.getLossRate()
                                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                        .divide(bom.getQty(), 6, RoundingMode.HALF_UP);
                BigDecimal childCost = roll(item.getProductId(), perUnit.multiply(qtyPerParent),
                        childLines, visiting, depth + 1);
                total = total.add(childCost);
            }
            lines.add(new CostLine(productId, names.get(productId), null, total,
                    qtyPerParent, "BOM 卷算 " + bom.getNo(), depth));
            lines.addAll(childLines);
            return total;
        } finally {
            visiting.remove(productId);
        }
    }

    public record CostLine(Long productId, String productName, BigDecimal unitPrice,
                           BigDecimal cost, BigDecimal qty, String source, int depth) {
    }

    public record RollupResult(Long productId, String productName, BigDecimal totalCost, List<CostLine> lines) {
    }
}
