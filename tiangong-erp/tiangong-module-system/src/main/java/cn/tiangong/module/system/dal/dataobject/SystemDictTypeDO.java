package cn.tiangong.module.system.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_dict_type")
public class SystemDictTypeDO extends BaseDO {

    private String name;

    private String type;

    private Integer status;
}
