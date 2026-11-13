package cn.tiangong.module.bas.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单号规则：{prefix}{datePart}{seq}，如 SO + 202611 + 0001。
 * resetType：none 不重置 / year 按年 / month 按月 / day 按天（通过 Redis key 携带日期实现隔离）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bas_serial_rule")
public class BasSerialRuleDO extends BaseDO {

    private String bizType;

    private String prefix;

    private String dateFormat;

    private Integer seqLength;

    private String resetType;

    private String remark;
}
