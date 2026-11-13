package cn.tiangong.module.fin.api;

/**
 * 财务 - 对外 API（供业务模块红字冲销等场景）。
 */
public interface FinApi {

    /** 该来源单据的应收是否存在且完全未核销（可冲销） */
    boolean isReceivableClean(String bizType, Long bizId);

    /** 该来源单据的应付是否存在且完全未核销（可冲销） */
    boolean isPayableClean(String bizType, Long bizId);

    /** 作废来源单据对应的应收（冲销联动） */
    void voidReceivableByBiz(String bizType, Long bizId);

    /** 作废来源单据对应的应付（冲销联动） */
    void voidPayableByBiz(String bizType, Long bizId);

    /** 生成原凭证的红字冲销凭证（借贷互换），返回新凭证号 */
    String reverseVoucher(String bizType, Long bizId);

    /** 该客户当前未核销应收总额 */
    java.math.BigDecimal unsettledReceivableOfPartner(Long partnerId);
}
