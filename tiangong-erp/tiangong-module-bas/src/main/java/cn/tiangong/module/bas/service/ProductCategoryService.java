package cn.tiangong.module.bas.service;

import cn.tiangong.module.bas.dal.dataobject.ErpProductCategoryDO;

import java.util.List;

public interface ProductCategoryService {

    List<ErpProductCategoryDO> getCategoryList(Integer status);

    ErpProductCategoryDO getCategory(Long id);

    Long createCategory(String name, Long parentId, Integer sort);

    void updateCategory(Long id, String name, Long parentId, Integer sort, Integer status);

    void deleteCategory(Long id);
}
