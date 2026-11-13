package cn.tiangong.module.bas.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.bas.dal.dataobject.ErpWarehouseDO;
import cn.tiangong.module.bas.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/bas/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/list")
    public CommonResult<List<ErpWarehouseDO>> getWarehouseList(
            @RequestParam(value = "status", required = false) Integer status) {
        return CommonResult.success(warehouseService.getWarehouseList(status));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bas:warehouse:create')")
    public CommonResult<Long> createWarehouse(@Valid @RequestBody WarehouseReqVO reqVO) {
        return CommonResult.success(warehouseService.createWarehouse(reqVO.toDO()));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('bas:warehouse:update')")
    public CommonResult<Boolean> updateWarehouse(@Valid @RequestBody WarehouseUpdateReqVO reqVO) {
        warehouseService.updateWarehouse(reqVO.toDO());
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('bas:warehouse:delete')")
    public CommonResult<Boolean> deleteWarehouse(@RequestParam("id") Long id) {
        warehouseService.deleteWarehouse(id);
        return CommonResult.success(true);
    }

    public record WarehouseReqVO(String no, String name, String address,
                                 Boolean isQualityCheck, String remark) {
        ErpWarehouseDO toDO() {
            ErpWarehouseDO d = new ErpWarehouseDO();
            d.setNo(no());
            d.setName(name());
            d.setAddress(address());
            d.setIsQualityCheck(Boolean.TRUE.equals(isQualityCheck()));
            d.setRemark(remark());
            return d;
        }
    }

    public record WarehouseUpdateReqVO(Long id, String name, String address,
                                       Boolean isQualityCheck, String remark) {
        ErpWarehouseDO toDO() {
            ErpWarehouseDO d = new ErpWarehouseDO();
            d.setId(id());
            d.setName(name());
            d.setAddress(address());
            d.setIsQualityCheck(isQualityCheck());
            d.setRemark(remark());
            return d;
        }
    }
}
