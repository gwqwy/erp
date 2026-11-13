package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款单：核销应付。审核 = 核销明细生效 + 自动生成凭证（借 应付账款 / 贷 银行存款）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_payment")
public class ErpFinancePaymentDO extends BaseDO {

    private String no;

    private Long partnerId;

    private Long accountId;

    private LocalDate paymentDate;

    private BigDecimal amount;

    private BigDecimal unallocatedAmount;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}
