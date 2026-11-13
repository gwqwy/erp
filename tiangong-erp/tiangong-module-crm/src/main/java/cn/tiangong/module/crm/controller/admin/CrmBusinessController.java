package cn.tiangong.module.crm.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.PartnerApi;
import cn.tiangong.module.crm.dal.dataobject.CrmBusinessDO;
import cn.tiangong.module.crm.service.business.BusinessService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/crm/business")
public class CrmBusinessController {

    private final BusinessService businessService;
    private final PartnerApi partnerApi;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    public CommonResult<PageResult<BusinessRespVO>> getBusinessPage(PageParam pageParam,
                                                                    @RequestParam(required = false) String name,
                                                                    @RequestParam(required = false) Long partnerId,
                                                                    @RequestParam(required = false) Integer stage) {
        PageResult<CrmBusinessDO> page = businessService.getBusinessPage(pageParam, name, partnerId, stage);
        Map<Long, String> names = partnerApi.getPartnerNames(
                page.getList().stream().map(CrmBusinessDO::getPartnerId).collect(Collectors.toSet()));
        return CommonResult.success(PageResult.of(page,
                b -> new BusinessRespVO(String.valueOf(b.getId()), b.getName(),
                        String.valueOf(b.getPartnerId()),
                        names.getOrDefault(b.getPartnerId(), "未知客户"),
                        b.getAmount(), b.getStage(), b.getExpectDate())));
    }

    @GetMapping("/stage-stats")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    public CommonResult<Map<Integer, long[]>> getStageStatistics() {
        return CommonResult.success(businessService.stageStatistics());
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('crm:business:create')")
    public CommonResult<Long> createBusiness(@Valid @RequestBody BusinessReqVO reqVO) {
        return CommonResult.success(businessService.createBusiness(reqVO.name(), reqVO.partnerId(),
                reqVO.amount(), reqVO.stage(), reqVO.expectDate(), reqVO.remark()));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('crm:business:update')")
    public CommonResult<Boolean> updateBusiness(@Valid @RequestBody BusinessUpdateReqVO reqVO) {
        businessService.updateBusiness(reqVO.id(), reqVO.name(), reqVO.partnerId(), reqVO.amount(),
                reqVO.expectDate(), reqVO.remark());
        return CommonResult.success(true);
    }

    @PutMapping("/stage")
    @PreAuthorize("@ss.hasPermission('crm:business:update')")
    public CommonResult<Boolean> updateStage(@RequestParam("id") Long id,
                                             @RequestParam("stage") Integer stage) {
        businessService.updateStage(id, stage);
        return CommonResult.success(true);
    }

    /** 赢单商机一键转销售订单草稿 */
    @PostMapping("/to-sale-order")
    @PreAuthorize("@ss.hasPermission('crm:business:update')")
    public CommonResult<Long> toSaleOrder(@RequestParam("id") Long id,
                                          @RequestParam("productId") Long productId,
                                          @RequestParam("qty") BigDecimal qty,
                                          @RequestParam(value = "price", required = false) BigDecimal price) {
        return CommonResult.success(businessService.toSaleOrder(id, productId, qty, price));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('crm:business:delete')")
    public CommonResult<Boolean> deleteBusiness(@RequestParam("id") Long id) {
        businessService.deleteBusiness(id);
        return CommonResult.success(true);
    }

    public record BusinessRespVO(String id, String name, String partnerId, String partnerName,
                                 BigDecimal amount, Integer stage, LocalDate expectDate) {
    }

    public record BusinessReqVO(String name, Long partnerId, BigDecimal amount, Integer stage,
                                LocalDate expectDate, String remark) {
    }

    public record BusinessUpdateReqVO(Long id, String name, Long partnerId, BigDecimal amount,
                                      LocalDate expectDate, String remark) {
    }
}
