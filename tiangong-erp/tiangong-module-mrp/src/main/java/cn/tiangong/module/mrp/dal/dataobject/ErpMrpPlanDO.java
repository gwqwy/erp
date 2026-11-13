package cn.tiangong.module.mrp.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MRP 计划：试算结果落库。resultJson 为叶子净需求行数组
 * [{productId, productName, stockQty, netQty}]。
 * status：1 已保存 / 2 已转采购。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_mrp_plan")
public class ErpMrpPlanDO extends BaseDO {

    private String no;

    private Long warehouseId;

    /** 需求入参 JSON：[{productId, qty}] */
    private String demandsJson;

    /** 结果 JSON：[{productId, productName, stockQty, netQty}] */
    private String resultJson;

    /** 转采购生成的订单 id，未转为空 */
    private Long purchaseOrderId;

    private Integer status;

    private String remark;
}
