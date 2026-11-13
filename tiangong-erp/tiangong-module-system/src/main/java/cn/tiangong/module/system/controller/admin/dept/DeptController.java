package cn.tiangong.module.system.controller.admin.dept;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.system.dal.dataobject.SystemDeptDO;
import cn.tiangong.module.system.service.dept.DeptService;
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
@RequestMapping("/admin-api/system/dept")
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/list")
    public CommonResult<List<SystemDeptDO>> getDeptList() {
        return CommonResult.success(deptService.getDeptList());
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:dept:create')")
    public CommonResult<Long> createDept(@RequestParam("name") @NotBlank(message = "部门名称不能为空") String name,
                                         @RequestParam(value = "parentId", required = false) Long parentId,
                                         @RequestParam(value = "leaderUserId", required = false) Long leaderUserId,
                                         @RequestParam(value = "sort", required = false) Integer sort) {
        return CommonResult.success(deptService.createDept(name, parentId, leaderUserId, sort));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> updateDept(@RequestParam("id") Long id,
                                            @RequestParam(value = "name", required = false) String name,
                                            @RequestParam(value = "parentId", required = false) Long parentId,
                                            @RequestParam(value = "leaderUserId", required = false) Long leaderUserId,
                                            @RequestParam(value = "sort", required = false) Integer sort,
                                            @RequestParam(value = "status", required = false) Integer status) {
        deptService.updateDept(id, name, parentId, leaderUserId, sort, status);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDept(@RequestParam("id") Long id) {
        deptService.deleteDept(id);
        return CommonResult.success(true);
    }
}
