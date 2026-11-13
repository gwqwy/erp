package cn.tiangong.module.system.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户。status：0 启用 / 1 停用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_users")
public class SystemUsersDO extends BaseDO {

    private String username;

    @JsonIgnore
    private String password;

    private String nickname;

    private Long deptId;

    private Integer status;
}
