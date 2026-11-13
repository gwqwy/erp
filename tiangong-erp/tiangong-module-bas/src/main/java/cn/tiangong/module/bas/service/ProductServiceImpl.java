package cn.tiangong.module.bas.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.dal.dataobject.ErpProductDO;
import cn.tiangong.module.bas.dal.mysql.ErpProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final ErpProductMapper productMapper;
    private final SerialNoService serialNoService;

    @Override
    public PageResult<ErpProductDO> getProductPage(PageParam pageParam, String name, String no,
                                                   Long categoryId, Integer status) {
        LambdaQueryWrapper<ErpProductDO> wrapper = new LambdaQueryWrapper<ErpProductDO>()
                .like(name != null && !name.isBlank(), ErpProductDO::getName, name)
                .like(no != null && !no.isBlank(), ErpProductDO::getNo, no)
                .eq(categoryId != null, ErpProductDO::getCategoryId, categoryId)
                .eq(status != null, ErpProductDO::getStatus, status)
                .orderByDesc(ErpProductDO::getId);
        return productMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public ErpProductDO getProduct(Long id) {
        ErpProductDO product = productMapper.selectById(id);
        if (product == null) {
            throw of(2_001, 1, "产品不存在");
        }
        return product;
    }

    @Override
    public Long createProduct(ErpProductDO product) {
        if (product.getNo() == null || product.getNo().isBlank()) {
            product.setNo(serialNoService.nextNo("erp_product"));
        }
        if (product.getStatus() == null) {
            product.setStatus(0);
        }
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    public void updateProduct(ErpProductDO product) {
        getProduct(product.getId());
        product.setNo(null); // 编码不允许修改
        productMapper.updateById(product);
    }

    @Override
    public void deleteProduct(Long id) {
        getProduct(id);
        productMapper.deleteById(id);
    }

    @Override
    public void updateProductStatus(Long id, Integer status) {
        ErpProductDO product = getProduct(id);
        product.setStatus(status);
        productMapper.updateById(product);
    }
}
