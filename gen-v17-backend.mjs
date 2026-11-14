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

// ================= 1. BOM/infra 加 POI =================

patch('tiangong-erp/tiangong-dependencies/pom.xml', s => {
  if (s.includes('poi-ooxml')) return s;
  return s.replace('</dependencyManagement>',
    `            <dependency>
                <groupId>org.apache.poi</groupId>
                <artifactId>poi-ooxml</artifactId>
                <version>5.2.5</version>
            </dependency>
        </dependencyManagement>`);
});
console.log('bom poi:', fs.readFileSync('tiangong-erp/tiangong-dependencies/pom.xml', 'utf8').includes('poi-ooxml'));

patch('tiangong-erp/tiangong-module-infra/pom.xml', s => {
  if (s.includes('poi-ooxml')) return s;
  return s.replace('<artifactId>tiangong-spring-boot-starter-web</artifactId>\n        </dependency>',
    `<artifactId>tiangong-spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
        </dependency>`);
});
console.log('infra poi:', fs.readFileSync('tiangong-erp/tiangong-module-infra/pom.xml', 'utf8').includes('poi-ooxml'));

// ================= 2. V17 SQL =================

write('tiangong-server/src/main/resources/db/migration/V17__excel_serial_deposit.sql', `-- =====================================================================
-- V17：Excel 导入导出 / 序列号追溯 / 订金管理
-- =====================================================================

-- 序列号台账
CREATE TABLE IF NOT EXISTS erp_serial_track (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  serial_no     VARCHAR(64)  NOT NULL COMMENT '序列号',
  product_id    BIGINT       NOT NULL,
  warehouse_id  BIGINT       NOT NULL DEFAULT 0,
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 在库 2 已出库',
  inbound_no    VARCHAR(32)           DEFAULT '' COMMENT '入库单号',
  inbound_time  DATETIME              NULL,
  outbound_no   VARCHAR(32)           DEFAULT '' COMMENT '出库单号',
  outbound_time DATETIME              NULL,
  batch_no      VARCHAR(64)           DEFAULT '',
  creator       VARCHAR(64)           DEFAULT '',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_serial_product (serial_no, product_id),
  KEY idx_product_status (product_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '序列号追溯';

-- 订金字段
ALTER TABLE erp_sale_order     ADD COLUMN deposit_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已收订金' AFTER remark;
ALTER TABLE erp_purchase_order ADD COLUMN deposit_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已付订金' AFTER remark;

-- 菜单：更多功能 → 序列号追溯
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15230, '序列号追溯', '', 2, 5, 15020, 'serial-trace', 'more/serial-trace/index'),
  (15231, '序列号查询', 'stk:serial:query', 3, 1, 15230, '', '');
`);

// ================= 3. 序列号 API（stk 模块） =================

write('tiangong-erp/tiangong-module-stk/src/main/java/cn/tiangong/module/stk/api/SerialTrackApi.java', `package cn.tiangong.module.stk.api;

import java.util.List;

/**
 * 序列号追溯 API：入库/出库审核时登记序列号（采购/销售模块调用）。
 */
public interface SerialTrackApi {

    /** 入库登记（status 在库）；已存在的序列号跳过 */
    void registerInbound(List<String> serialNos, Long productId, Long warehouseId, String inboundNo);

    /** 出库核销（在库 → 已出库）；未登记的自动补记后直接置为已出库 */
    void consumeOutbound(List<String> serialNos, Long productId, Long warehouseId, String outboundNo);
}
`);

write('tiangong-erp/tiangong-module-stk/src/main/java/cn/tiangong/module/stk/api/impl/SerialTrackApiImpl.java', `package cn.tiangong.module.stk.api.impl;

import cn.tiangong.module.stk.api.SerialTrackApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 序列号追溯实现：直接 SQL 写 erp_serial_track（与过账同事务，由调用方事务包裹）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SerialTrackApiImpl implements SerialTrackApi {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void registerInbound(List<String> serialNos, Long productId, Long warehouseId, String inboundNo) {
        for (String sn : serialNos) {
            String no = sn.trim();
            if (no.isEmpty()) continue;
            // 已存在（任何状态）则跳过，避免重复入库报错
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM erp_serial_track WHERE serial_no = ? AND product_id = ?",
                    Integer.class, no, productId);
            if (cnt != null && cnt > 0) {
                log.info("[registerInbound] 序列号已存在，跳过: {} / {}", no, productId);
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO erp_serial_track (serial_no, product_id, warehouse_id, status, inbound_no, inbound_time, creator) " +
                            "VALUES (?, ?, ?, 1, ?, ?, 'system')",
                    no, productId, warehouseId == null ? 0L : warehouseId, inboundNo, Timestamp.valueOf(LocalDateTime.now()));
        }
    }

    @Override
    public void consumeOutbound(List<String> serialNos, Long productId, Long warehouseId, String outboundNo) {
        for (String sn : serialNos) {
            String no = sn.trim();
            if (no.isEmpty()) continue;
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM erp_serial_track WHERE serial_no = ? AND product_id = ?",
                    Integer.class, no, productId);
            if (cnt == null || cnt == 0) {
                // 未登记直接出库（历史数据/跳过登记）：补记全轨迹
                jdbcTemplate.update(
                        "INSERT INTO erp_serial_track (serial_no, product_id, warehouse_id, status, outbound_no, outbound_time, creator) " +
                                "VALUES (?, ?, ?, 2, ?, ?, 'system')",
                        no, productId, warehouseId == null ? 0L : warehouseId, outboundNo, Timestamp.valueOf(LocalDateTime.now()));
                continue;
            }
            jdbcTemplate.update(
                    "UPDATE erp_serial_track SET status = 2, outbound_no = ?, outbound_time = ? " +
                            "WHERE serial_no = ? AND product_id = ? AND status = 1",
                    outboundNo, Timestamp.valueOf(LocalDateTime.now()), no, productId);
        }
    }
}
`);

// ================= 4. 订金 API（sal / pur） =================

write('tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/controller/admin/SaleOrderDepositController.java', `package cn.tiangong.module.sal.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOrderDO;
import cn.tiangong.module.sal.dal.mysql.ErpSaleOrderMapper;
import cn.tiangong.module.sal.service.order.SaleOrderService;
import cn.tiangong.framework.web.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** 销售订金：登记（累计）/ 查询。订金作为收款单外的预收跟踪，金额不得超过订单总额。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/sal/order")
public class SaleOrderDepositController {

    private final ErpSaleOrderMapper orderMapper;
    private final SaleOrderService orderService;

    @GetMapping("/deposit")
    @PreAuthorize("@ss.hasPermission('sal:order:query')")
    public CommonResult<ErpSaleOrderDO> getDeposit(@RequestParam("id") Long id) {
        return CommonResult.success(orderMapper.selectById(id));
    }

    @PostMapping("/deposit")
    @PreAuthorize("@ss.hasPermission('sal:order:audit')")
    public CommonResult<BigDecimal> registerDeposit(@RequestParam("id") Long id,
                                                    @RequestParam("amount") BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ServiceException.of(7_02, 1, "订金金额必须大于 0");
        }
        ErpSaleOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw ServiceException.of(7_02, 2, "订单不存在");
        }
        BigDecimal total = order.getTotalAmountTax() == null ? order.getTotalAmount() : order.getTotalAmountTax();
        BigDecimal newDeposit = order.getDepositAmount().add(amount);
        if (newDeposit.compareTo(total) > 0) {
            throw ServiceException.of(7_02, 3, "累计订金 " + newDeposit + " 超过订单总额 " + total);
        }
        orderService.updateDeposit(id, newDeposit);
        return CommonResult.success(newDeposit);
    }
}
`);

write('tiangong-erp/tiangong-module-pur/src/main/java/cn/tiangong/module/pur/controller/admin/PurchaseOrderDepositController.java', `package cn.tiangong.module.pur.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderDO;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseOrderMapper;
import cn.tiangong.module.pur.service.order.PurchaseOrderService;
import cn.tiangong.framework.web.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** 采购订金：登记（累计）/ 查询。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/pur/order")
public class PurchaseOrderDepositController {

    private final ErpPurchaseOrderMapper orderMapper;
    private final PurchaseOrderService orderService;

    @GetMapping("/deposit")
    @PreAuthorize("@ss.hasPermission('pur:order:query')")
    public CommonResult<ErpPurchaseOrderDO> getDeposit(@RequestParam("id") Long id) {
        return CommonResult.success(orderMapper.selectById(id));
    }

    @PostMapping("/deposit")
    @PreAuthorize("@ss.hasPermission('pur:order:audit')")
    public CommonResult<BigDecimal> registerDeposit(@RequestParam("id") Long id,
                                                    @RequestParam("amount") BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ServiceException.of(6_02, 1, "订金金额必须大于 0");
        }
        ErpPurchaseOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw ServiceException.of(6_02, 2, "订单不存在");
        }
        BigDecimal total = order.getTotalAmountTax() == null ? order.getTotalAmount() : order.getTotalAmountTax();
        BigDecimal newDeposit = order.getDepositAmount().add(amount);
        if (newDeposit.compareTo(total) > 0) {
            throw ServiceException.of(6_02, 3, "累计订金 " + newDeposit + " 超过订单总额 " + total);
        }
        orderService.updateDeposit(id, newDeposit);
        return CommonResult.success(newDeposit);
    }
}
`);

// service 接口 + 实现：updateDeposit
patch('tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/service/order/SaleOrderService.java', s => {
  if (s.includes('updateDeposit')) return s;
  return s.replace('    void voidOrder(Long id);', `    void voidOrder(Long id);

    /** 更新累计订金 */
    void updateDeposit(Long id, java.math.BigDecimal depositAmount);`);
});
patch('tiangong-erp/tiangong-module-pur/src/main/java/cn/tiangong/module/pur/service/order/PurchaseOrderService.java', s => {
  if (s.includes('updateDeposit')) return s;
  return s.replace('    void voidOrder(Long id);', `    void voidOrder(Long id);

    /** 更新累计订金 */
    void updateDeposit(Long id, java.math.BigDecimal depositAmount);`);
});

// impl：在 void 方法附近追加 updateDeposit 实现（锚定 voidXXX 实现）
patch('tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/service/order/SaleOrderServiceImpl.java', s => {
  if (s.includes('public void updateDeposit')) return s;
  const anchor = '    public void voidOrder(';
  const i = s.indexOf(anchor);
  if (i < 0) throw new Error('sale impl voidOrder not found');
  const insert = `    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeposit(Long id, java.math.BigDecimal depositAmount) {
        ErpSaleOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw of(7_01, 2, "订单不存在: " + id);
        }
        order.setDepositAmount(depositAmount);
        orderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "订金更新为 " + depositAmount, null);
    }

`;
  return s.slice(0, i) + insert + s.slice(i);
});
patch('tiangong-erp/tiangong-module-pur/src/main/java/cn/tiangong/module/pur/service/order/PurchaseOrderServiceImpl.java', s => {
  if (s.includes('public void updateDeposit')) return s;
  const anchor = '    public void voidOrder(';
  const i = s.indexOf(anchor);
  if (i < 0) throw new Error('pur impl voidOrder not found');
  const insert = `    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeposit(Long id, java.math.BigDecimal depositAmount) {
        ErpPurchaseOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw of(6_01, 2, "订单不存在: " + id);
        }
        order.setDepositAmount(depositAmount);
        orderMapper.updateById(order);
        chatterService.trace(BIZ_TYPE, id, "订金更新为 " + depositAmount, null);
    }

`;
  return s.slice(0, i) + insert + s.slice(i);
});

console.log('deposit wiring done');

// ================= 5. DO 补 depositAmount 字段 =================

patch('tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/dal/dataobject/ErpSaleOrderDO.java', s => {
  if (s.includes('depositAmount')) return s;
  return s.replace('    private String remark;', '    private String remark;\n\n    private java.math.BigDecimal depositAmount;');
});
patch('tiangong-erp/tiangong-module-pur/src/main/java/cn/tiangong/module/pur/dal/dataobject/ErpPurchaseOrderDO.java', s => {
  if (s.includes('depositAmount')) return s;
  return s.replace('    private String remark;', '    private String remark;\n\n    private java.math.BigDecimal depositAmount;');
});
console.log('DO deposit fields added');

// ================= 6. 序列号钩子：入库/出库审核解析 remark =================

patch('tiangong-erp/tiangong-module-pur/src/main/java/cn/tiangong/module/pur/service/inbound/PurchaseInboundServiceImpl.java', s => {
  if (s.includes('serialTrackApi')) return s;
  // 注入
  s = s.replace('    private final StockPostingApi stockPostingApi;',
    `    private final StockPostingApi stockPostingApi;
    private final cn.tiangong.module.stk.api.SerialTrackApi serialTrackApi;`);
  // 钩子：库存过账后
  s = s.replace('        stockPostingApi.submit(commands);',
    `        stockPostingApi.submit(commands);

        // 序列号登记：isSerial 商品明细 remark 中的序列号（逗号/空格分隔）
        registerSerialsIfAny(inbound.getWarehouseId(), inbound.getId(), inbound.getNo(), items);`);
  // 追加私有方法（文件末尾类结束前）
  const tail = s.lastIndexOf('}');
  const helper = `
    private void registerSerialsIfAny(Long warehouseId, Long inboundId, String inboundNo,
                                      java.util.List<cn.tiangong.module.pur.dal.dataobject.ErpPurchaseInboundItemDO> items) {
        try {
            for (ErpPurchaseInboundItemDO item : items) {
                java.util.List<String> sns = parseSerials(item.getRemark());
                if (sns.isEmpty()) continue;
                serialTrackApi.registerInbound(sns, item.getProductId(), warehouseId, inboundNo);
            }
        } catch (Exception e) {
            cn.tiangong.framework.web.core.exception.ServiceException.of(6_04, 9,
                    "序列号登记失败: " + e.getMessage());
        }
    }

    private java.util.List<String> parseSerials(String remark) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (remark == null || remark.isBlank()) return out;
        for (String token : remark.split("[,，;；\\\\s]+")) {
            String t = token.trim();
            if (!t.isEmpty() && t.length() >= 3) out.add(t);
        }
        return out;
    }
`;
  return s.slice(0, tail) + helper + s.slice(tail);
});
console.log('pur serial hook:', fs.readFileSync('tiangong-erp/tiangong-module-pur/src/main/java/cn/tiangong/module/pur/service/inbound/PurchaseInboundServiceImpl.java', 'utf8').includes('serialTrackApi'));

patch('tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/service/outbound/SaleOutboundServiceImpl.java', s => {
  if (s.includes('serialTrackApi')) return s;
  s = s.replace('    private final StockPostingApi stockPostingApi;',
    `    private final StockPostingApi stockPostingApi;
    private final cn.tiangong.module.stk.api.SerialTrackApi serialTrackApi;`);
  s = s.replace('        stockPostingApi.submit(commands);',
    `        stockPostingApi.submit(commands);

        // 序列号核销：isSerial 商品明细 remark 中的序列号
        consumeSerialsIfAny(outbound.getWarehouseId(), outbound.getId(), outbound.getNo(), items);`);
  const tail = s.lastIndexOf('}');
  const helper = `
    private void consumeSerialsIfAny(Long warehouseId, Long outboundId, String outboundNo,
                                     java.util.List<cn.tiangong.module.sal.dal.dataobject.ErpSaleOutboundItemDO> items) {
        try {
            for (ErpSaleOutboundItemDO item : items) {
                java.util.List<String> sns = parseSerials(item.getRemark());
                if (sns.isEmpty()) continue;
                serialTrackApi.consumeOutbound(sns, item.getProductId(), warehouseId, outboundNo);
            }
        } catch (Exception e) {
            throw cn.tiangong.framework.web.core.exception.ServiceException.of(7_04, 9,
                    "序列号核销失败: " + e.getMessage());
        }
    }

    private java.util.List<String> parseSerials(String remark) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (remark == null || remark.isBlank()) return out;
        for (String token : remark.split("[,，;；\\\\s]+")) {
            String t = token.trim();
            if (!t.isEmpty() && t.length() >= 3) out.add(t);
        }
        return out;
    }
`;
  return s.slice(0, tail) + helper + s.slice(tail);
});
console.log('sal serial hook:', fs.readFileSync('tiangong-erp/tiangong-module-sal/src/main/java/cn/tiangong/module/sal/service/outbound/SaleOutboundServiceImpl.java', 'utf8').includes('serialTrackApi'));

console.log('gen-v17-backend part1 done');
