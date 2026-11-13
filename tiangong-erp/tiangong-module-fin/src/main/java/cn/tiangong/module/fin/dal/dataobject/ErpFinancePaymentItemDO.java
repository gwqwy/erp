package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 付款核销明细：一付多核（一张付款单可核销多张应付）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_payment_item")
public class ErpFinancePaymentItemDO extends BaseDO {

    private Long paymentId;

    private Long payableId;

    private BigDecimal amount;

    private String remark;
}
