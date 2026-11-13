package cn.tiangong.module.stk.job;

import cn.tiangong.module.stk.dal.mysql.StockAlertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 库存预警定时任务：每天 08:00 扫描低库存，命中则给超级管理员发站内消息。
 * 也可通过 /admin-api/stk/alert/scan 手动触发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertJob {

    private final StockAlertMapper alertMapper;

    /** 扫描一次，返回低库存商品列表 */
    public List<Map<String, Object>> scanOnce() {
        return alertMapper.lowStockProducts();
    }

    /** 扫描并发站内消息，返回 {count, notified} */
    public Map<String, Object> scanAndNotify() {
        List<Map<String, Object>> lows = scanOnce();
        boolean notified = false;
        if (!lows.isEmpty()) {
            Long userId = alertMapper.firstUserId();
            if (userId != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(lows.size(), 10); i++) {
                    Map<String, Object> row = lows.get(i);
                    sb.append(i + 1).append(". ")
                            .append(row.get("productName"))
                            .append("（库存 ").append(row.get("totalQty"))
                            .append(" / 安全量 ").append(row.get("stockMin")).append("）");
                    if (i < Math.min(lows.size(), 10) - 1) sb.append("\n");
                }
                if (lows.size() > 10) sb.append("\n… 等共 ").append(lows.size()).append(" 个商品");
                alertMapper.insertMessage(userId, "库存预警："
                        + lows.size() + " 个商品低于安全库存", sb.toString());
                notified = true;
            }
        }
        return Map.of("count", lows.size(), "notified", notified);
    }

    /** 每天 08:00 自动扫描 */
    @Scheduled(cron = "0 0 8 * * ?")
    public void dailyScan() {
        try {
            Map<String, Object> result = scanAndNotify();
            if (Boolean.TRUE.equals(result.get("notified"))) {
                log.info("[dailyScan] 库存预警已推送：{} 个商品", result.get("count"));
            }
        } catch (Exception e) {
            log.warn("[dailyScan] 扫描失败: {}", e.getMessage());
        }
    }
}
