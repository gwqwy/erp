package cn.tiangong.module.system.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色-菜单关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_role_menu")
public class SystemRoleMenuDO extends BaseDO {

    private Long roleId;

    private Long menuId;
}
