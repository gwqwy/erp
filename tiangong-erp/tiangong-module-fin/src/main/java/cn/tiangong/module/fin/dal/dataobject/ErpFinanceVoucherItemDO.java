package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 凭证分录：借贷不同负，合计必平。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_voucher_item")
public class ErpFinanceVoucherItemDO extends BaseDO {

    private Long voucherId;

    private Long subjectId;

    private String subjectNo;

    private String summary;

    private BigDecimal debit;

    private BigDecimal credit;

    private Long partnerId;
}
