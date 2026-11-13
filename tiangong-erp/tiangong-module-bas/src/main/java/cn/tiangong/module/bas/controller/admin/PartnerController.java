package cn.tiangong.module.bas.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.dal.dataobject.ErpPartnerDO;
import cn.tiangong.module.bas.service.PartnerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

import java.math.BigDecimal;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/bas/partner")
public class PartnerController {

    private final PartnerService partnerService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('bas:partner:query')")
    public CommonResult<PageResult<ErpPartnerDO>> getPartnerPage(PageParam pageParam,
                                                                 @RequestParam(required = false) String name,
                                                                 @RequestParam(required = false) Boolean isCustomer,
                                                                 @RequestParam(required = false) Boolean isSupplier,
                                                                 @RequestParam(required = false) Integer status) {
        return CommonResult.success(partnerService.getPartnerPage(pageParam, name, isCustomer, isSupplier, status));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('bas:partner:query')")
    public CommonResult<ErpPartnerDO> getPartner(@RequestParam("id") Long id) {
        return CommonResult.success(partnerService.getPartner(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bas:partner:create')")
    public CommonResult<Long> createPartner(@Valid @RequestBody PartnerReqVO reqVO) {
        return CommonResult.success(partnerService.createPartner(reqVO.toDO()));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('bas:partner:update')")
    public CommonResult<Boolean> updatePartner(@Valid @RequestBody PartnerUpdateReqVO reqVO) {
        partnerService.updatePartner(reqVO.toDO());
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('bas:partner:delete')")
    public CommonResult<Boolean> deletePartner(@RequestParam("id") Long id) {
        partnerService.deletePartner(id);
        return CommonResult.success(true);
    }

    public record PartnerReqVO(@NotBlank(message = "名称不能为空") String name, String shortName,
                               Boolean isCustomer, Boolean isSupplier, String taxNo, String address,
                               String contact, String phone, BigDecimal creditLimit, String remark,
                               String no) {
        ErpPartnerDO toDO() {
            ErpPartnerDO d = new ErpPartnerDO();
            d.setNo(no());
            d.setName(name());
            d.setShortName(shortName());
            d.setIsCustomer(Boolean.TRUE.equals(isCustomer()));
            d.setIsSupplier(Boolean.TRUE.equals(isSupplier()));
            d.setTaxNo(taxNo());
            d.setAddress(address());
            d.setContact(contact());
            d.setPhone(phone());
            d.setCreditLimit(creditLimit());
            d.setRemark(remark());
            return d;
        }
    }

    public record PartnerUpdateReqVO(Long id, String name, String shortName, Boolean isCustomer,
                                     Boolean isSupplier, String taxNo, String address, String contact,
                                     String phone, BigDecimal creditLimit, String remark) {
        ErpPartnerDO toDO() {
            ErpPartnerDO d = new ErpPartnerDO();
            d.setId(id());
            d.setName(name());
            d.setShortName(shortName());
            d.setIsCustomer(isCustomer());
            d.setIsSupplier(isSupplier());
            d.setTaxNo(taxNo());
            d.setAddress(address());
            d.setContact(contact());
            d.setPhone(phone());
            d.setCreditLimit(creditLimit());
            d.setRemark(remark());
            return d;
        }
    }
}
