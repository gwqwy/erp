package cn.tiangong.module.sal.api;

import java.util.List;
import java.util.Map;

/**
 * 销售 - 统计对外 API（报表模块专用，只读）。
 */
public interface SalStatApi {

    /** 近 N 个月已审核销售出库金额序列：[{month:"2026-06", amount:123}, ...]（升序） */
    List<Map<String, Object>> monthlySalesAmount(int months);
}
