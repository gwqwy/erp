package cn.tiangong.module.mrp.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ErpMrpBomMapper extends BaseMapperX<ErpMrpBomDO> {

    /**
     * 物理删除：BOM 有 (product_id, version) 唯一键，逻辑删除的行会持续占用键位，
     * 导致同版本无法重建，故 BOM 采用物理删除。
     */
    @Delete("DELETE FROM erp_mrp_bom WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM erp_mrp_bom WHERE product_id = #{productId} "
            + "AND version = #{version} AND company_id = 1")
    long countByProductVersion(@Param("productId") Long productId, @Param("version") Integer version);
}
