package cn.tiangong.module.system.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户-角色关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_user_role")
public class SystemUserRoleDO extends BaseDO {

    private Long userId;

    private Long roleId;
}
