package cn.tiangong.module.bas.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.dal.dataobject.ErpProductDO;

public interface ProductService {

    PageResult<ErpProductDO> getProductPage(PageParam pageParam, String name, String no,
                                            Long categoryId, Integer status);

    ErpProductDO getProduct(Long id);

    /** 创建产品，no 为空时按规则自动生成（规则 bizType = erp_product） */
    Long createProduct(ErpProductDO product);

    void updateProduct(ErpProductDO product);

    void deleteProduct(Long id);

    void updateProductStatus(Long id, Integer status);
}
