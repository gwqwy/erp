package cn.tiangong.module.mrp.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomItemDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface ErpMrpBomItemMapper extends BaseMapperX<ErpMrpBomItemDO> {

    @Delete("DELETE FROM erp_mrp_bom_item WHERE bom_id = #{bomId}")
    int physicalDeleteByBomId(@Param("bomId") Long bomId);
}
