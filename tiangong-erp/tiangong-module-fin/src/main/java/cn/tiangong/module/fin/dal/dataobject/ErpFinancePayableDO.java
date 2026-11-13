package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应付台账：采购入库审核后自动生成；付款单审核核销。
 * status：1 未核销 2 部分核销 3 已结清 4 已作废。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_payable")
public class ErpFinancePayableDO extends BaseDO {

    /** 来源业务类型：purchase_in / other */
    private String bizType;

    private Long bizId;

    private String bizNo;

    private Long partnerId;

    private BigDecimal amount;

    /** 已核销金额（付款单审核回写） */
    private BigDecimal paidAmount;

    private LocalDate dueDate;

    private Integer status;

    private LocalDate settleDate;
}
