package cn.tiangong.module.bas.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 仓库。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_warehouse")
public class ErpWarehouseDO extends BaseDO {

    private String no;

    private String name;

    private String address;

    private Boolean isQualityCheck;

    private Integer status;

    private String remark;
}
