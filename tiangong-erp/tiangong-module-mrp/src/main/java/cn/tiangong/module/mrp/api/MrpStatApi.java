package cn.tiangong.module.mrp.api;

/**
 * 生产 - 统计对外 API（报表模块专用，只读）。
 */
public interface MrpStatApi {

    /** 进行中的工单数量（已下达/生产中） */
    long activeWorkOrders();
}
