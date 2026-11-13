package cn.tiangong.module.bas.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.bas.dal.dataobject.ErpLocationDO;
import cn.tiangong.module.bas.dal.dataobject.ErpPartnerPriceDO;
import cn.tiangong.module.bas.dal.mysql.ErpLocationMapper;
import cn.tiangong.module.bas.dal.mysql.ErpPartnerPriceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** 库位管理 + 客户/供应商价格策略（基础数据扩展）。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/bas")
public class BasExtController {

    private final cn.tiangong.module.bas.dal.mysql.ErpLocationMapper locationMapper;
    private final cn.tiangong.module.bas.dal.mysql.ErpPartnerPriceMapper priceMapper;

    // ========== 库位 ==========

    @GetMapping("/location/list")
    @PreAuthorize("@ss.hasPermission('bas:location:query')")
    public CommonResult<List<ErpLocationDO>> getLocationList(@RequestParam("warehouseId") Long warehouseId) {
        return CommonResult.success(locationMapper.selectList(new LambdaQueryWrapper<ErpLocationDO>()
                .eq(ErpLocationDO::getWarehouseId, warehouseId)
                .orderByAsc(ErpLocationDO::getNo)));
    }

    @PostMapping("/location/create")
    @PreAuthorize("@ss.hasPermission('bas:location:create')")
    public CommonResult<Long> createLocation(@RequestParam("warehouseId") Long warehouseId,
                                             @RequestParam("no") String no,
                                             @RequestParam("name") String name) {
        ErpLocationDO location = new ErpLocationDO();
        location.setWarehouseId(warehouseId);
        location.setNo(no);
        location.setName(name);
        location.setStatus(0);
        locationMapper.insert(location);
        return CommonResult.success(location.getId());
    }

    @PutMapping("/location/update")
    @PreAuthorize("@ss.hasPermission('bas:location:update')")
    public CommonResult<Boolean> updateLocation(@RequestParam("id") Long id,
                                                @RequestParam("name") String name,
                                                @RequestParam(value = "status", required = false) Integer status) {
        ErpLocationDO location = locationMapper.selectById(id);
        if (location == null) {
            return CommonResult.error(404, "库位不存在");
        }
        location.setName(name);
        if (status != null) location.setStatus(status);
        locationMapper.updateById(location);
        return CommonResult.success(true);
    }

    @DeleteMapping("/location/delete")
    @PreAuthorize("@ss.hasPermission('bas:location:delete')")
    public CommonResult<Boolean> deleteLocation(@RequestParam("id") Long id) {
        locationMapper.deleteById(id);
        return CommonResult.success(true);
    }

    // ========== 价格策略 ==========

    @GetMapping("/price/list")
    @PreAuthorize("@ss.hasPermission('bas:price:query')")
    public CommonResult<List<ErpPartnerPriceDO>> getPriceList(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) Long productId) {
        return CommonResult.success(priceMapper.selectList(new LambdaQueryWrapper<ErpPartnerPriceDO>()
                .eq(partnerId != null, ErpPartnerPriceDO::getPartnerId, partnerId)
                .eq(productId != null, ErpPartnerPriceDO::getProductId, productId)
                .orderByDesc(ErpPartnerPriceDO::getId)));
    }

    @PostMapping("/price/create")
    @PreAuthorize("@ss.hasPermission('bas:price:create')")
    public CommonResult<Long> createPrice(@RequestParam("partnerId") Long partnerId,
                                          @RequestParam("productId") Long productId,
                                          @RequestParam("price") BigDecimal price,
                                          @RequestParam(value = "remark", required = false) String remark) {
        ErpPartnerPriceDO pp = new ErpPartnerPriceDO();
        pp.setPartnerId(partnerId);
        pp.setProductId(productId);
        pp.setPrice(price);
        pp.setRemark(remark);
        priceMapper.insert(pp);
        return CommonResult.success(pp.getId());
    }

    @DeleteMapping("/price/delete")
    @PreAuthorize("@ss.hasPermission('bas:price:delete')")
    public CommonResult<Boolean> deletePrice(@RequestParam("id") Long id) {
        priceMapper.deleteById(id);
        return CommonResult.success(true);
    }
}
