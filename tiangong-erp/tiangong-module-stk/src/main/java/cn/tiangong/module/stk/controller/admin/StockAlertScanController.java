package cn.tiangong.module.stk.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.stk.dal.mysql.StockAlertMapper;
import cn.tiangong.module.stk.job.StockAlertJob;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 库存预警：手动扫描 + 预览低库存列表。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/stk/alert")
public class StockAlertScanController {

    private final StockAlertJob alertJob;
    private final StockAlertMapper alertMapper;

    /** 手动扫描（等同每日 08:00 定时任务），命中低库存则发站内消息 */
    @PostMapping("/scan")
    @PreAuthorize("@ss.hasPermission('stk:quant:query')")
    public CommonResult<Map<String, Object>> scanNow() {
        return CommonResult.success(alertJob.scanAndNotify());
    }

    /** 低库存列表（含安全量对比） */
    @GetMapping("/low-list")
    @PreAuthorize("@ss.hasPermission('stk:quant:query')")
    public CommonResult<List<Map<String, Object>>> lowList() {
        return CommonResult.success(alertMapper.lowStockProducts());
    }
}
