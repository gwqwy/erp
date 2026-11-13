package cn.tiangong.module.system.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色。code = super_admin 的角色拥有全部权限（PermissionChecker 通配放行）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_role")
public class SystemRoleDO extends BaseDO {

    private String name;

    private String code;

    private Integer sort;

    private Integer status;

    private String remark;
}
