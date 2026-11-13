package cn.tiangong.module.bpm.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bpm.dal.dataobject.BpmApprovalConfigDO;
import cn.tiangong.module.bpm.dal.dataobject.BpmApprovalTaskDO;
import cn.tiangong.module.bpm.service.ApprovalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
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
@RequestMapping("/admin-api/bpm")
public class BpmController {

    private final ApprovalService approvalService;

    @GetMapping("/config/exists")
    public CommonResult<Boolean> hasApprovalFlow(@RequestParam("bizType") String bizType) {
        return CommonResult.success(approvalService.hasApprovalFlow(bizType));
    }

    @GetMapping("/my-tasks")
    public CommonResult<List<BpmApprovalTaskDO>> myPendingTasks() {
        return CommonResult.success(approvalService.myPendingTasks(
                cn.tiangong.framework.security.util.SecurityFrameworkUtils.getLoginUserId()));
    }

    @GetMapping("/task/page")
    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
    public CommonResult<PageResult<BpmApprovalTaskDO>> getTaskPage(PageParam pageParam,
                                                                   @RequestParam(required = false) String bizType,
                                                                   @RequestParam(required = false) Long approverUserId,
                                                                   @RequestParam(required = false) Integer status) {
        return CommonResult.success(approvalService.getTaskPage(pageParam, bizType, approverUserId, status));
    }

    @PostMapping("/approve")
    public CommonResult<Boolean> approve(@Valid @RequestBody ApproveReqVO reqVO) {
        approvalService.approve(reqVO.getTaskId(), reqVO.getComment());
        return CommonResult.success(true);
    }

    @PostMapping("/reject")
    public CommonResult<Boolean> reject(@Valid @RequestBody ApproveReqVO reqVO) {
        approvalService.reject(reqVO.getTaskId(), reqVO.getComment());
        return CommonResult.success(true);
    }

    // ===== 审批配置 =====

    @GetMapping("/config/page")
    @PreAuthorize("@ss.hasPermission('bpm:config:query')")
    public CommonResult<PageResult<BpmApprovalConfigDO>> getConfigPage(PageParam pageParam,
                                                                       @RequestParam(required = false) String bizType) {
        return CommonResult.success(approvalService.getConfigPage(pageParam, bizType));
    }

    @PostMapping("/config/create")
    @PreAuthorize("@ss.hasPermission('bpm:config:create')")
    public CommonResult<Long> createConfig(@RequestParam("bizType") @NotBlank String bizType,
                                           @RequestParam("level") @NotNull Integer level,
                                           @RequestParam("approverUserId") @NotNull Long approverUserId) {
        return CommonResult.success(approvalService.createConfig(bizType, level, approverUserId));
    }

    @PutMapping("/config/update")
    @PreAuthorize("@ss.hasPermission('bpm:config:update')")
    public CommonResult<Boolean> updateConfig(@RequestParam("id") Long id,
                                              @RequestParam(value = "level", required = false) Integer level,
                                              @RequestParam(value = "approverUserId", required = false) Long approverUserId,
                                              @RequestParam(value = "enabled", required = false) Integer enabled) {
        approvalService.updateConfig(id, level, approverUserId, enabled);
        return CommonResult.success(true);
    }

    @DeleteMapping("/config/delete")
    @PreAuthorize("@ss.hasPermission('bpm:config:delete')")
    public CommonResult<Boolean> deleteConfig(@RequestParam("id") Long id) {
        approvalService.deleteConfig(id);
        return CommonResult.success(true);
    }

    @Data
    public static class ApproveReqVO {
        @NotNull(message = "任务编号不能为空")
        private Long taskId;
        private String comment;
    }
}
