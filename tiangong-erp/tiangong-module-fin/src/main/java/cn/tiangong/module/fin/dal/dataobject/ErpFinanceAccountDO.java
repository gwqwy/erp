package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 结算账户（现金/银行）。balance 由收付款审核联动更新（M2 简化：不做对账单）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_account")
public class ErpFinanceAccountDO extends BaseDO {

    private String no;

    private String name;

    /** 1 现金 2 银行 3 其他 */
    private Integer type;

    private BigDecimal balance;

    private Integer status;
}
