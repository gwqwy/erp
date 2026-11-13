package cn.tiangong.module.crm.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.crm.dal.dataobject.CrmLeadDO;
import cn.tiangong.module.crm.service.lead.LeadService;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/crm/lead")
public class CrmLeadController {

    private final LeadService leadService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('crm:lead:query')")
    public CommonResult<PageResult<CrmLeadDO>> getLeadPage(PageParam pageParam,
                                                           @RequestParam(required = false) String name,
                                                           @RequestParam(required = false) Integer status) {
        return CommonResult.success(leadService.getLeadPage(pageParam, name, status));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('crm:lead:create')")
    public CommonResult<Long> createLead(@Valid @RequestBody LeadReqVO reqVO) {
        return CommonResult.success(leadService.createLead(reqVO.name(), reqVO.source(),
                reqVO.contact(), reqVO.phone(), reqVO.remark()));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('crm:lead:update')")
    public CommonResult<Boolean> updateLead(@Valid @RequestBody LeadUpdateReqVO reqVO) {
        leadService.updateLead(reqVO.id(), reqVO.name(), reqVO.source(), reqVO.contact(),
                reqVO.phone(), reqVO.remark());
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('crm:lead:delete')")
    public CommonResult<Boolean> deleteLead(@RequestParam("id") Long id) {
        leadService.deleteLead(id);
        return CommonResult.success(true);
    }

    @PostMapping("/convert")
    @PreAuthorize("@ss.hasPermission('crm:lead:convert')")
    public CommonResult<Long> convertToBusiness(@RequestParam("id") Long id,
                                                @RequestParam("partnerId") Long partnerId,
                                                @RequestParam(value = "amount", required = false) BigDecimal amount,
                                                @RequestParam(value = "expectDate", required = false) LocalDate expectDate) {
        return CommonResult.success(leadService.convertToBusiness(id, partnerId, amount, expectDate));
    }

    public record LeadReqVO(String name, String source, String contact, String phone, String remark) {
    }

    public record LeadUpdateReqVO(Long id, String name, String source, String contact,
                                  String phone, String remark) {
    }
}
