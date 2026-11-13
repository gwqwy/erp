package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款单：核销应收。审核 = 核销明细生效 + 自动生成凭证（借 银行存款 / 贷 应收账款）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_receipt")
public class ErpFinanceReceiptDO extends BaseDO {

    private String no;

    private Long partnerId;

    private Long accountId;

    private LocalDate receiptDate;

    private BigDecimal amount;

    /** 剩余未分配金额（amount - Σ明细），M2 要求全部分配后才能审核 */
    private BigDecimal unallocatedAmount;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}
