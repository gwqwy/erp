package cn.tiangong.module.system.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单/权限。type：1 目录 / 2 菜单 / 3 按钮；按钮类菜单的 permission 即权限标识。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_menu")
public class SystemMenuDO extends BaseDO {

    private String name;

    private String permission;

    private Integer type;

    private Integer sort;

    private Long parentId;

    private String path;

    private String component;

    private Integer status;
}
