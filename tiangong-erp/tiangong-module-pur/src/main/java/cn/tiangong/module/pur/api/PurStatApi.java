package cn.tiangong.module.pur.api;

import java.util.List;
import java.util.Map;

/**
 * 采购 - 统计对外 API（报表模块专用，只读）。
 */
public interface PurStatApi {

    /** 近 N 个月已审核采购入库金额序列（升序） */
    List<Map<String, Object>> monthlyPurchaseAmount(int months);
}
