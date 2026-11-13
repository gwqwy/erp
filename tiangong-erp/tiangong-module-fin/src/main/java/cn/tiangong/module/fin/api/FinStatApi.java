package cn.tiangong.module.fin.api;

import java.math.BigDecimal;

/**
 * 财务 - 统计对外 API（报表模块专用，只读）。
 */
public interface FinStatApi {

    /** 未核销应收总额 */
    BigDecimal unsettledReceivable();

    /** 未核销应付总额 */
    BigDecimal unsettledPayable();
}
