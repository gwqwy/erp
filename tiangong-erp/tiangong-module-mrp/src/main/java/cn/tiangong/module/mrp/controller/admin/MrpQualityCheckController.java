package cn.tiangong.module.mrp.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.ProductApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpQualityCheckDO;
import cn.tiangong.module.mrp.dal.mysql.ErpQualityCheckMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/admin-api/mrp/quality-check")
public class MrpQualityCheckController {

    private final ErpQualityCheckMapper checkMapper;
    private final ProductApi productApi;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mrp:quality:query')")
    public CommonResult<PageResult<CheckRespVO>> getCheckPage(PageParam pageParam,
                                                              @RequestParam(required = false) String checkType,
                                                              @RequestParam(required = false) Integer result) {
        LambdaQueryWrapper<ErpQualityCheckDO> wrapper = new LambdaQueryWrapper<ErpQualityCheckDO>()
                .eq(checkType != null && !checkType.isBlank(), ErpQualityCheckDO::getCheckType, checkType)
                .eq(result != null, ErpQualityCheckDO::getResult, result)
                .orderByDesc(ErpQualityCheckDO::getId);
        PageResult<ErpQualityCheckDO> page = checkMapper.selectPage(pageParam, wrapper);
        Map<Long, String> names = productApi.getProductNames(
                page.getList().stream().map(ErpQualityCheckDO::getProductId).collect(Collectors.toSet()));
        return CommonResult.success(PageResult.of(page,
                c -> new CheckRespVO(String.valueOf(c.getId()), c.getNo(), c.getCheckType(), c.getSourceNo(),
                        String.valueOf(c.getProductId()), names.getOrDefault(c.getProductId(), "未知产品"),
                        c.getCheckQty(), c.getQualifiedQty(), c.getInspector(),
                        String.valueOf(c.getCheckDate()), c.getResult(), c.getRemark())));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mrp:quality:create')")
    public CommonResult<Long> createCheck(@RequestParam("no") @NotBlank String no,
                                          @RequestParam("checkType") @NotBlank String checkType,
                                          @RequestParam("productId") @NotNull Long productId,
                                          @RequestParam("checkQty") @NotNull BigDecimal checkQty,
                                          @RequestParam("qualifiedQty") @NotNull BigDecimal qualifiedQty,
                                          @RequestParam(value = "sourceNo", required = false) String sourceNo,
                                          @RequestParam(value = "inspector", required = false) String inspector,
                                          @RequestParam(value = "result", required = false) Integer result,
                                          @RequestParam(value = "remark", required = false) String remark) {
        ErpQualityCheckDO check = new ErpQualityCheckDO();
        check.setNo(no);
        check.setCheckType(checkType);
        check.setProductId(productId);
        check.setCheckQty(checkQty);
        check.setQualifiedQty(qualifiedQty);
        check.setSourceNo(sourceNo);
        check.setInspector(inspector);
        check.setCheckDate(LocalDate.now());
        check.setResult(result == null ? 1 : result);
        check.setRemark(remark);
        checkMapper.insert(check);
        return CommonResult.success(check.getId());
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('mrp:quality:delete')")
    public CommonResult<Boolean> deleteCheck(@RequestParam("id") Long id) {
        checkMapper.deleteById(id);
        return CommonResult.success(true);
    }

    public record CheckRespVO(String id, String no, String checkType, String sourceNo,
                              String productId, String productName, BigDecimal checkQty,
                              BigDecimal qualifiedQty, String inspector, String checkDate,
                              Integer result, String remark) {
    }
}
