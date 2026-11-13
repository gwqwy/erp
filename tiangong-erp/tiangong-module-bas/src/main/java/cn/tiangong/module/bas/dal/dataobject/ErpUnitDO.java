package cn.tiangong.module.bas.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 计量单位（个/箱/kg…）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_unit")
public class ErpUnitDO extends BaseDO {

    private String name;

    private Integer status;
}
