package cn.tiangong.module.stk.api.impl;

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
