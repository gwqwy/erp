package cn.tiangong.module.system.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_dept")
public class SystemDeptDO extends BaseDO {

    private String name;

    private Long parentId;

    private Integer sort;

    private Long leaderUserId;

    private Integer status;
}
