package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用登记（审核生成凭证：借 6601 / 贷 1001）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_expense")
public class ErpFinanceExpenseDO extends BaseDO {

    private String no;

    private LocalDate expenseDate;

    private String category;

    private BigDecimal amount;

    private Long accountId;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}
