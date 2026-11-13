package cn.tiangong.module.infra.controller.admin;

import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.infra.dal.dataobject.InfraNoticeDO;
import cn.tiangong.module.infra.dal.mysql.InfraNoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/infra/notice")
public class InfraNoticeController {

    private final InfraNoticeMapper noticeMapper;

    /** 全员可读：已发布公告列表 */
    @GetMapping("/list")
    public CommonResult<List<InfraNoticeDO>> getNoticeList() {
        return CommonResult.success(noticeMapper.selectList(new LambdaQueryWrapper<InfraNoticeDO>()
                .eq(InfraNoticeDO::getStatus, 0)
                .orderByDesc(InfraNoticeDO::getId)));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('infra:notice:create')")
    public CommonResult<Long> createNotice(@RequestParam("title") @NotBlank String title,
                                           @RequestParam(value = "content", required = false) String content) {
        InfraNoticeDO notice = new InfraNoticeDO();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setPublishUser(SecurityFrameworkUtils.getLoginUser() != null
                ? SecurityFrameworkUtils.getLoginUser().getUsername() : "");
        notice.setPublishTime(LocalDateTime.now());
        notice.setStatus(0);
        noticeMapper.insert(notice);
        return CommonResult.success(notice.getId());
    }

    /** 撤回 */
    @PutMapping("/withdraw")
    @PreAuthorize("@ss.hasPermission('infra:notice:delete')")
    public CommonResult<Boolean> withdraw(@RequestParam("id") Long id) {
        InfraNoticeDO notice = noticeMapper.selectById(id);
        if (notice == null) {
            return CommonResult.error(404, "公告不存在");
        }
        notice.setStatus(1);
        noticeMapper.updateById(notice);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('infra:notice:delete')")
    public CommonResult<Boolean> deleteNotice(@RequestParam("id") Long id) {
        noticeMapper.deleteById(id);
        return CommonResult.success(true);
    }
}
