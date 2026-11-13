package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会计期间：结转后锁账，期间内单据禁止再审核。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_period")
public class ErpFinancePeriodDO extends BaseDO {

    private String period;

    private Boolean closed;

    private LocalDateTime closeTime;

    private String closeUser;
}
