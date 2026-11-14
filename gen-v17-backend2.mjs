import fs from 'node:fs';
import path from 'node:path';

const base = '.';

function write(rel, content) {
  const fp = path.join(base, rel);
  fs.mkdirSync(path.dirname(fp), { recursive: true });
  fs.writeFileSync(fp, content, 'utf8');
  console.log('write:', rel);
}

function patch(rel, fn) {
  const fp = path.join(base, rel);
  let s = fs.readFileSync(fp, 'utf8');
  s = fn(s);
  fs.writeFileSync(fp, s, 'utf8');
}

// ================= 1. 序列号查询控制器 =================

write('tiangong-erp/tiangong-module-stk/src/main/java/cn/tiangong/module/stk/controller/admin/StockSerialController.java', `package cn.tiangong.module.stk.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 序列号追溯：时间线查询 / 在库列表 / 手工登记与核销。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/stk/serial")
public class StockSerialController {

    private final JdbcTemplate jdbcTemplate;

    /** 按序列号查轨迹（含商品名） */
    @GetMapping("/trace")
    @PreAuthorize("@ss.hasPermission('stk:serial:query')")
    public CommonResult<List<Map<String, Object>>> trace(@RequestParam("serialNo") String serialNo) {
        return CommonResult.success(jdbcTemplate.queryForList(
                "SELECT t.*, p.name AS product_name, p.no AS product_no " +
                        "FROM erp_serial_track t LEFT JOIN erp_product p ON p.id = t.product_id " +
                        "WHERE t.serial_no = ? ORDER BY t.id ASC", serialNo));
    }

    /** 按商品查在库序列号 */
    @GetMapping("/in-stock")
    @PreAuthorize("@ss.hasPermission('stk:serial:query')")
    public CommonResult<List<Map<String, Object>>> inStock(@RequestParam("productId") Long productId) {
        return CommonResult.success(jdbcTemplate.queryForList(
                "SELECT * FROM erp_serial_track WHERE product_id = ? AND status = 1 ORDER BY id ASC", productId));
    }

    /** 手工登记 */
    @PostMapping("/register")
    @PreAuthorize("@ss.hasPermission('stk:serial:query')")
    public CommonResult<Boolean> register(@RequestParam("serialNo") String serialNo,
                                          @RequestParam("productId") Long productId,
                                          @RequestParam(value = "warehouseId", required = false) Long warehouseId,
                                          @RequestParam(value = "bizNo", required = false) String bizNo) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM erp_serial_track WHERE serial_no = ? AND product_id = ?",
                Integer.class, serialNo, productId);
        if (cnt != null && cnt > 0) {
            return CommonResult.error(400, "该序列号已存在");
        }
        jdbcTemplate.update(
                "INSERT INTO erp_serial_track (serial_no, product_id, warehouse_id, status, inbound_no, inbound_time, creator) " +
                        "VALUES (?, ?, ?, 1, ?, ?, 'manual')",
                serialNo, productId, warehouseId == null ? 0L : warehouseId, bizNo,
                Timestamp.valueOf(LocalDateTime.now()));
        return CommonResult.success(true);
    }

    /** 手工核销 */
    @PostMapping("/consume")
    @PreAuthorize("@ss.hasPermission('stk:serial:query')")
    public CommonResult<Boolean> consume(@RequestParam("serialNo") String serialNo,
                                         @RequestParam("productId") Long productId,
                                         @RequestParam(value = "bizNo", required = false) String bizNo) {
        int n = jdbcTemplate.update(
                "UPDATE erp_serial_track SET status = 2, outbound_no = ?, outbound_time = ? " +
                        "WHERE serial_no = ? AND product_id = ? AND status = 1",
                bizNo, Timestamp.valueOf(LocalDateTime.now()), serialNo, productId);
        return CommonResult.success(n > 0);
    }
}
`);

// ================= 2. BOM 成本卷算 =================

write('tiangong-erp/tiangong-module-mrp/src/main/java/cn/tiangong/module/mrp/service/cost/BomCostService.java', `package cn.tiangong.module.mrp.service.cost;

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
        Map<Long, String> names = productApi.getProductNames(Set.of(productId));
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
                BigDecimal price = productApi.getPurchasePrices(List.of(productId))
                        .getOrDefault(productId, BigDecimal.ZERO);
                lines.add(new CostLine(productId, null, price, price.multiply(qtyPerParent),
                        qtyPerParent, "原材料", depth));
                return price.multiply(qtyPerParent);
            }
            // 组件行展开
            List<ErpMrpBomItemDO> items = bomService.getBomDetail(bom.getId()).items();
            BigDecimal total = BigDecimal.ZERO;
            Map<Long, String> names = productApi.getProductNames(
                    items.stream().map(ErpMrpBomItemDO::getProductId).collect(java.util.stream.Collectors.toSet()));
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
`);

// ProductApi 是否有 getPurchasePrices？没有则补
const prodApiPath = 'tiangong-erp/tiangong-module-bas/src/main/java/cn/tiangong/module/bas/api/ProductApi.java';
let prodApi = fs.readFileSync(prodApiPath, 'utf8');
if (!prodApi.includes('getPurchasePrices')) {
  prodApi = prodApi.replace(/\}\s*$/, `
    /** 批量取采购价（用于 BOM 成本卷算） */
    java.util.Map<Long, java.math.BigDecimal> getPurchasePrices(java.util.Set<Long> productIds);
}
`);
  fs.writeFileSync(prodApiPath, prodApi, 'utf8');
}
console.log('ProductApi getPurchasePrices added:', prodApi.includes('getPurchasePrices'));

// ProductApiImpl 实现
const prodApiImplPath = 'tiangong-erp/tiangong-module-bas/src/main/java/cn/tiangong/module/bas/api/impl/ProductApiImpl.java';
let impl = fs.readFileSync(prodApiImplPath, 'utf8');
if (!impl.includes('getPurchasePrices')) {
  // 找到类结尾前插入实现
  const tail = impl.lastIndexOf('}');
  const method = `
    @Override
    public java.util.Map<Long, java.math.BigDecimal> getPurchasePrices(java.util.Set<Long> productIds) {
        java.util.Map<Long, java.math.BigDecimal> out = new java.util.HashMap<>();
        if (productIds == null || productIds.isEmpty()) return out;
        for (cn.tiangong.module.bas.dal.dataobject.ErpProductDO p : productMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.tiangong.module.bas.dal.dataobject.ErpProductDO>()
                        .in(cn.tiangong.module.bas.dal.dataobject.ErpProductDO::getId, productIds))) {
            out.put(p.getId(), p.getPurchasePrice() == null ? java.math.BigDecimal.ZERO : p.getPurchasePrice());
        }
        return out;
    }
`;
  impl = impl.slice(0, tail) + method + impl.slice(tail);
  fs.writeFileSync(prodApiImplPath, impl, 'utf8');
}
console.log('ProductApiImpl impl added:', impl.includes('getPurchasePrices'));

// MrpCalcController 加 cost 端点
patch('tiangong-erp/tiangong-module-mrp/src/main/java/cn/tiangong/module/mrp/controller/admin/MrpCalcController.java', s => {
  if (s.includes('/cost')) return s;
  s = s.replace('    private final MrpCalcService calcService;',
    `    private final MrpCalcService calcService;
    private final cn.tiangong.module.mrp.service.cost.BomCostService bomCostService;`);
  return s.trimEnd().replace(/\n}\n?$/, `

    /** BOM 成本卷算（默认 BOM 递归 + 叶子采购价） */
    @GetMapping("/cost")
    @PreAuthorize("@ss.hasPermission('mrp:calc:query')")
    public CommonResult<cn.tiangong.module.mrp.service.cost.BomCostService.RollupResult> cost(
            @org.springframework.web.bind.annotation.RequestParam("productId") Long productId) {
        return CommonResult.success(bomCostService.rollup(productId));
    }
}
`);
});
console.log('calc controller cost endpoint added');

// mrp 依赖 bas（应该已有）——检查
const mrpPom = fs.readFileSync('tiangong-erp/tiangong-module-mrp/pom.xml', 'utf8');
console.log('mrp has bas dep:', mrpPom.includes('tiangong-module-bas'));

console.log('gen-v17-backend2 done');
