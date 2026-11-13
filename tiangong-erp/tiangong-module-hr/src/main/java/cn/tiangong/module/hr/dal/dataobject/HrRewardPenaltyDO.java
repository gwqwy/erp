package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 奖惩记录：type reward 奖励 / penalty 处罚（算薪时取正负号）。
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_reward_penalty")
public class HrRewardPenaltyDO extends BaseDO {

    private String no;

    private Long employeeId;

    private String type;

    private BigDecimal amount;

    private LocalDate rpDate;

    private String reason;

}
