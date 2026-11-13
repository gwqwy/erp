package cn.tiangong.module.system.controller.admin;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.system.dal.dataobject.SystemOperLogDO;
import cn.tiangong.module.system.dal.mysql.SystemOperLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/system/oper-log")
public class OperLogController {

    private final SystemOperLogMapper operLogMapper;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('system:operlog:query')")
    public CommonResult<PageResult<SystemOperLogDO>> getLogPage(PageParam pageParam,
                                                                @RequestParam(required = false) Long userId,
                                                                @RequestParam(required = false) Boolean success) {
        LambdaQueryWrapper<SystemOperLogDO> wrapper = new LambdaQueryWrapper<SystemOperLogDO>()
                .eq(userId != null, SystemOperLogDO::getUserId, userId)
                .eq(success != null, SystemOperLogDO::getSuccess, success)
                .orderByDesc(SystemOperLogDO::getId);
        return CommonResult.success(operLogMapper.selectPage(pageParam, wrapper));
    }
}
