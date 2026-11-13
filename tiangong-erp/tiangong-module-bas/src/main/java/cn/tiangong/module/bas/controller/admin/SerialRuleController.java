package cn.tiangong.module.bas.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.dal.dataobject.BasSerialRuleDO;
import cn.tiangong.module.bas.service.SerialNoService;
import jakarta.validation.constraints.NotBlank;
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

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/bas/serial-rule")
public class SerialRuleController {

    private final SerialNoService serialNoService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('bas:serial-rule:query')")
    public CommonResult<PageResult<BasSerialRuleDO>> getRulePage(PageParam pageParam,
                                                                 @RequestParam(required = false) String bizType) {
        return CommonResult.success(serialNoService.getRulePage(pageParam, bizType));
    }

    @GetMapping("/list")
    public CommonResult<List<BasSerialRuleDO>> getRuleList() {
        return CommonResult.success(serialNoService.getRuleList());
    }

    @GetMapping("/preview")
    public CommonResult<String> previewNo(@RequestParam("bizType") String bizType) {
        return CommonResult.success(serialNoService.previewNo(bizType));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('bas:serial-rule:create')")
    public CommonResult<Long> createRule(@RequestParam("bizType") @NotBlank(message = "业务类型不能为空") String bizType,
                                         @RequestParam("prefix") @NotBlank(message = "前缀不能为空") String prefix,
                                         @RequestParam(value = "dateFormat", required = false) String dateFormat,
                                         @RequestParam(value = "seqLength", required = false) Integer seqLength,
                                         @RequestParam(value = "resetType", required = false) String resetType,
                                         @RequestParam(value = "remark", required = false) String remark) {
        return CommonResult.success(serialNoService.createRule(bizType, prefix, dateFormat, seqLength, resetType, remark));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('bas:serial-rule:update')")
    public CommonResult<Boolean> updateRule(@RequestParam("id") Long id,
                                            @RequestParam(value = "prefix", required = false) String prefix,
                                            @RequestParam(value = "dateFormat", required = false) String dateFormat,
                                            @RequestParam(value = "seqLength", required = false) Integer seqLength,
                                            @RequestParam(value = "resetType", required = false) String resetType,
                                            @RequestParam(value = "remark", required = false) String remark) {
        serialNoService.updateRule(id, prefix, dateFormat, seqLength, resetType, remark);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('bas:serial-rule:delete')")
    public CommonResult<Boolean> deleteRule(@RequestParam("id") Long id) {
        serialNoService.deleteRule(id);
        return CommonResult.success(true);
    }
}
