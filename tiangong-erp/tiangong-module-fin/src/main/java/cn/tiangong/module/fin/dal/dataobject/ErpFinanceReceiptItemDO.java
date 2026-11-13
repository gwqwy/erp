package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 收款核销明细：一付多核（一张收款单可核销多张应收）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_receipt_item")
public class ErpFinanceReceiptItemDO extends BaseDO {

    private Long receiptId;

    private Long receivableId;

    /** 本次核销金额 */
    private BigDecimal amount;

    private String remark;
}
