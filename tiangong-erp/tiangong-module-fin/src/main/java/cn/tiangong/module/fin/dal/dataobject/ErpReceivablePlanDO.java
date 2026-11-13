package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_receivable_plan")
public class ErpReceivablePlanDO extends BaseDO {

    private String no;

    private Long partnerId;

    private LocalDate planDate;

    private BigDecimal amount;

    private Integer status;

    private String remark;
}
