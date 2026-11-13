package cn.tiangong.module.bas.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品分类（支持树形）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_product_category")
public class ErpProductCategoryDO extends BaseDO {

    private String name;

    private Long parentId;

    private Integer sort;

    private Integer status;
}
