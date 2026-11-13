package cn.tiangong.module.bas.service;

import cn.tiangong.module.bas.dal.dataobject.ErpProductCategoryDO;
import cn.tiangong.module.bas.dal.dataobject.ErpProductDO;
import cn.tiangong.module.bas.dal.mysql.ErpProductCategoryMapper;
import cn.tiangong.module.bas.dal.mysql.ErpProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ErpProductCategoryMapper categoryMapper;
    private final ErpProductMapper productMapper;

    @Override
    public List<ErpProductCategoryDO> getCategoryList(Integer status) {
        return categoryMapper.selectList(new LambdaQueryWrapper<ErpProductCategoryDO>()
                .eq(status != null, ErpProductCategoryDO::getStatus, status)
                .orderByAsc(ErpProductCategoryDO::getSort));
    }

    @Override
    public ErpProductCategoryDO getCategory(Long id) {
        ErpProductCategoryDO category = categoryMapper.selectById(id);
        if (category == null) {
            throw of(2_002, 1, "产品分类不存在");
        }
        return category;
    }

    @Override
    public Long createCategory(String name, Long parentId, Integer sort) {
        ErpProductCategoryDO category = new ErpProductCategoryDO();
        category.setName(name);
        category.setParentId(parentId != null ? parentId : 0L);
        category.setSort(sort != null ? sort : 0);
        category.setStatus(0);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(Long id, String name, Long parentId, Integer sort, Integer status) {
        ErpProductCategoryDO category = getCategory(id);
        category.setName(name != null ? name : category.getName());
        category.setParentId(parentId != null ? parentId : category.getParentId());
        category.setSort(sort != null ? sort : category.getSort());
        category.setStatus(status != null ? status : category.getStatus());
        categoryMapper.updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        getCategory(id);
        if (categoryMapper.selectCount(ErpProductCategoryDO::getParentId, id) > 0) {
            throw of(2_002, 2, "存在子分类，无法删除");
        }
        if (productMapper.selectCount(ErpProductDO::getCategoryId, id) > 0) {
            throw of(2_002, 3, "分类下存在产品，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
