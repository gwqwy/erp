package cn.tiangong.module.infra.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.bas.dal.dataobject.ErpPartnerDO;
import cn.tiangong.module.bas.dal.dataobject.ErpProductDO;
import cn.tiangong.module.bas.dal.dataobject.ErpWarehouseDO;
import cn.tiangong.module.bas.dal.mysql.ErpPartnerMapper;
import cn.tiangong.module.bas.dal.mysql.ErpProductMapper;
import cn.tiangong.module.bas.dal.mysql.ErpWarehouseMapper;
import cn.tiangong.module.stk.dal.dataobject.StockQuantDO;
import cn.tiangong.module.stk.dal.mysql.StockQuantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 数据导入：商品 / 往来单位 / 库存期初。
 * CSV 编码 UTF-8，首行可为表头（含「编码」或「名称」时自动跳过），列以英文逗号分隔。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/infra/import")
public class BulkImportController {

    private final ErpProductMapper productMapper;
    private final ErpPartnerMapper partnerMapper;
    private final ErpWarehouseMapper warehouseMapper;
    private final StockQuantMapper quantMapper;

    /** 商品导入：编码,名称,规格,采购价,销售价,安全库存 */
    @PostMapping("/products")
    @PreAuthorize("@ss.hasPermission('infra:import:create')")
    public CommonResult<ImportResult> importProducts(@RequestParam("file") MultipartFile file) throws IOException {
        List<String[]> rows = readCsv(file);
        ImportResult result = new ImportResult(rows.size(), 0, 0, new ArrayList<>());
        for (int i = 0; i < rows.size(); i++) {
            String[] cells = rows.get(i);
            try {
                if (cells.length < 2) throw new IllegalArgumentException("至少需要 编码,名称 两列");
                String no = cell(cells, 0);
                String name = cell(cells, 1);
                if (no.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("编码/名称为空");
                ErpProductDO product = productMapper.selectOne(new LambdaQueryWrapper<ErpProductDO>()
                        .eq(ErpProductDO::getNo, no).last("LIMIT 1"));
                boolean created = false;
                if (product == null) {
                    product = new ErpProductDO();
                    product.setNo(no);
                    product.setName(name);
                    product.setStatus(0);
                    created = true;
                }
                product.setName(name);
                if (cells.length > 2 && !cell(cells, 2).isEmpty()) product.setSpec(cell(cells, 2));
                if (cells.length > 3 && !cell(cells, 3).isEmpty()) product.setPurchasePrice(num(cells[3]));
                if (cells.length > 4 && !cell(cells, 4).isEmpty()) product.setSalePrice(num(cells[4]));
                if (cells.length > 5 && !cell(cells, 5).isEmpty()) product.setStockMin(num(cells[5]));
                if (created) productMapper.insert(product);
                else productMapper.updateById(product);
                result.success++;
            } catch (Exception e) {
                result.fail++;
                result.errors.add("第 " + (i + 1) + " 行: " + e.getMessage());
            }
        }
        return CommonResult.success(result);
    }

    /** 往来单位导入：名称,类型(customer|supplier|both),联系人,电话,地址,备注 */
    @PostMapping("/partners")
    @PreAuthorize("@ss.hasPermission('infra:import:create')")
    public CommonResult<ImportResult> importPartners(@RequestParam("file") MultipartFile file) throws IOException {
        List<String[]> rows = readCsv(file);
        ImportResult result = new ImportResult(rows.size(), 0, 0, new ArrayList<>());
        for (int i = 0; i < rows.size(); i++) {
            String[] cells = rows.get(i);
            try {
                String name = cell(cells, 0);
                if (name.isEmpty()) throw new IllegalArgumentException("名称为空");
                String type = cells.length > 1 ? cell(cells, 1).toLowerCase() : "customer";
                ErpPartnerDO partner = partnerMapper.selectOne(new LambdaQueryWrapper<ErpPartnerDO>()
                        .eq(ErpPartnerDO::getName, name).last("LIMIT 1"));
                boolean created = false;
                if (partner == null) {
                    partner = new ErpPartnerDO();
                    partner.setNo("IM" + System.currentTimeMillis() % 10000000000L + (i % 97));
                    partner.setName(name);
                    partner.setStatus(0);
                    partner.setIsCustomer(false);
                    partner.setIsSupplier(false);
                    created = true;
                }
                if (type.equals("customer") || type.equals("both")) partner.setIsCustomer(true);
                if (type.equals("supplier") || type.equals("both")) partner.setIsSupplier(true);
                if (cells.length > 2 && !cell(cells, 2).isEmpty()) partner.setContact(cell(cells, 2));
                if (cells.length > 3 && !cell(cells, 3).isEmpty()) partner.setPhone(cell(cells, 3));
                if (cells.length > 4 && !cell(cells, 4).isEmpty()) partner.setAddress(cell(cells, 4));
                if (cells.length > 5 && !cell(cells, 5).isEmpty()) partner.setRemark(cell(cells, 5));
                if (created) partnerMapper.insert(partner);
                else partnerMapper.updateById(partner);
                result.success++;
            } catch (Exception e) {
                result.fail++;
                result.errors.add("第 " + (i + 1) + " 行: " + e.getMessage());
            }
        }
        return CommonResult.success(result);
    }

    /** 库存期初导入：仓库编码,商品编码,数量,成本价 */
    @PostMapping("/stock-openings")
    @PreAuthorize("@ss.hasPermission('infra:import:create')")
    public CommonResult<ImportResult> importStockOpenings(@RequestParam("file") MultipartFile file) throws IOException {
        List<String[]> rows = readCsv(file);
        ImportResult result = new ImportResult(rows.size(), 0, 0, new ArrayList<>());
        for (int i = 0; i < rows.size(); i++) {
            String[] cells = rows.get(i);
            try {
                if (cells.length < 3) throw new IllegalArgumentException("至少需要 仓库编码,商品编码,数量 三列");
                // 编码或名称匹配（对无编码的存量数据友好）
                String whKey = cell(cells, 0);
                ErpWarehouseDO warehouse = warehouseMapper.selectOne(new LambdaQueryWrapper<ErpWarehouseDO>()
                        .and(w -> w.eq(ErpWarehouseDO::getNo, whKey).or().eq(ErpWarehouseDO::getName, whKey))
                        .last("LIMIT 1"));
                if (warehouse == null) throw new IllegalArgumentException("仓库不存在: " + whKey);
                String prodKey = cell(cells, 1);
                ErpProductDO product = productMapper.selectOne(new LambdaQueryWrapper<ErpProductDO>()
                        .and(w -> w.eq(ErpProductDO::getNo, prodKey).or().eq(ErpProductDO::getName, prodKey))
                        .last("LIMIT 1"));
                if (product == null) throw new IllegalArgumentException("商品不存在: " + prodKey);
                BigDecimal qty = num(cells[2]);
                BigDecimal cost = cells.length > 3 && !cell(cells, 3).isEmpty() ? num(cells[3]) : BigDecimal.ZERO;
                StockQuantDO quant = quantMapper.selectOne(new LambdaQueryWrapper<StockQuantDO>()
                        .eq(StockQuantDO::getWarehouseId, warehouse.getId())
                        .eq(StockQuantDO::getProductId, product.getId())
                        .last("LIMIT 1"));
                if (quant == null) {
                    quant = new StockQuantDO();
                    quant.setWarehouseId(warehouse.getId());
                    quant.setProductId(product.getId());
                    quant.setBatchNo("INIT");
                    quant.setQty(qty);
                    quant.setLockedQty(BigDecimal.ZERO);
                    quant.setAvgCost(cost);
                    quant.setVersion(0);
                    quantMapper.insert(quant);
                } else {
                    quant.setQty(quant.getQty().add(qty));
                    if (cost.signum() > 0) quant.setAvgCost(cost);
                    quantMapper.updateById(quant);
                }
                result.success++;
            } catch (Exception e) {
                result.fail++;
                result.errors.add("第 " + (i + 1) + " 行: " + e.getMessage());
            }
        }
        return CommonResult.success(result);
    }

    // ========== helpers ==========

    private List<String[]> readCsv(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) content = content.substring(1);
        List<String[]> rows = new ArrayList<>();
        String[] lines = content.split("\r?\n");
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] cells = line.split(",", -1);
            for (int i = 0; i < cells.length; i++) cells[i] = cells[i].trim();
            // 表头行：首列含中文表头关键字则跳过
            if (rows.isEmpty() && (cells[0].contains("编码") || cells[0].contains("名称"))) continue;
            rows.add(cells);
        }
        return rows;
    }

    private String cell(String[] cells, int idx) {
        return idx < cells.length ? cells[idx] : "";
    }

    private BigDecimal num(String s) {
        return new BigDecimal(s.trim());
    }

    @lombok.Data
    public static class ImportResult {
        private int total;
        private int success;
        private int fail;
        private List<String> errors;

        public ImportResult(int total, int success, int fail, List<String> errors) {
            this.total = total;
            this.success = success;
            this.fail = fail;
            this.errors = errors;
        }
    }
}
