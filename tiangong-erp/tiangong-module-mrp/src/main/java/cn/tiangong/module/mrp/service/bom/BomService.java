package cn.tiangong.module.mrp.service.bom;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomItemDO;

import java.math.BigDecimal;
import java.util.List;

public interface BomService {

    PageResult<ErpMrpBomDO> getBomPage(PageParam pageParam, Long productId, Integer status);

    BomDetail getBomDetail(Long id);

    Long createBom(Long productId, Integer version, BigDecimal qty, String remark, List<Item> items);

    void updateBom(Long id, Long productId, Integer version, BigDecimal qty, String remark, List<Item> items);

    void deleteBom(Long id);

    /** 启用并设为该产品的默认 BOM（同产品其他版本取消默认） */
    void enableBom(Long id);

    /** 取产品默认启用的 BOM，不存在返回 null */
    ErpMrpBomDO getDefaultBom(Long productId);

    /**
     * 按 BOM 展开到全部叶子原材料（含损耗），供工单材料需求与 MRP 使用。
     *
     * @return 叶子行（同产品已聚合）
     */
    List<ErpMrpBomItemDO> expandToLeaves(Long productId, BigDecimal productQty);

    record Item(Long productId, BigDecimal qty, BigDecimal lossRate, String remark) {
    }

    record BomDetail(ErpMrpBomDO bom, List<ErpMrpBomItemDO> items) {
    }
}
