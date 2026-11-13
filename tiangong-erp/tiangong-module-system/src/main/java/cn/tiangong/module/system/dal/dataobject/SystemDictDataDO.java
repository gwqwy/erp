package cn.tiangong.module.system.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_dict_data")
public class SystemDictDataDO extends BaseDO {

    private String dictType;

    private String label;

    private String value;

    private Integer sort;

    private Integer status;
}
