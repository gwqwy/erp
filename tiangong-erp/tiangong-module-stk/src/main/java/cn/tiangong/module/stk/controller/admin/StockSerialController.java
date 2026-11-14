package cn.tiangong.module.stk.controller.admin;

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
                "SELECT t.id AS id, t.serial_no AS serialNo, t.product_id AS productId, t.status AS status, " +
                        "t.inbound_no AS inboundNo, t.inbound_time AS inboundTime, " +
                        "t.outbound_no AS outboundNo, t.outbound_time AS outboundTime, " +
                        "p.name AS productName, p.no AS productNo " +
                        "FROM erp_serial_track t LEFT JOIN erp_product p ON p.id = t.product_id " +
                        "WHERE t.serial_no = ? ORDER BY t.id ASC", serialNo));
    }

    /** 按商品查在库序列号 */
    @GetMapping("/in-stock")
    @PreAuthorize("@ss.hasPermission('stk:serial:query')")
    public CommonResult<List<Map<String, Object>>> inStock(@RequestParam("productId") Long productId) {
        return CommonResult.success(jdbcTemplate.queryForList(
                "SELECT id AS id, serial_no AS serialNo, product_id AS productId, status AS status, " +
                        "inbound_no AS inboundNo, inbound_time AS inboundTime FROM erp_serial_track " +
                        "WHERE product_id = ? AND status = 1 ORDER BY id ASC", productId));
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
