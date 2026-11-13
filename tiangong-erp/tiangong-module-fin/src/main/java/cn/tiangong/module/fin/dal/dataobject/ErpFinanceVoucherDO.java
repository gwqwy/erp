package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 总账凭证：业务单据审核后按模板自动生成（借方合计 = 贷方合计），也可手工录入（M3）。
 * status：1 草稿 2 已过账 3 已作废。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_voucher")
public class ErpFinanceVoucherDO extends BaseDO {

    private String no;

    private LocalDate voucherDate;

    /** 会计期间 yyyy-MM */
    private String period;

    /** 来源业务类型，手工凭证为空 */
    private String bizType;

    private Long bizId;

    private String sourceNo;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private Integer status;

    private LocalDateTime postedTime;
}
