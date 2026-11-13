package cn.tiangong.module.bas.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * 基础数据 - 往来单位对外 API。
 */
public interface PartnerApi {

    Map<Long, String> getPartnerNames(Collection<Long> partnerIds);

    /** 客户信用额度，未设置返回 null（不限制） */
    BigDecimal getCreditLimit(Long partnerId);
}
