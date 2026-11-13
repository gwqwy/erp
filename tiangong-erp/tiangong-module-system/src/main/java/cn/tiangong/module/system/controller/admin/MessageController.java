package cn.tiangong.module.system.controller.admin;

import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.system.dal.dataobject.SystemMessageDO;
import cn.tiangong.module.system.dal.mysql.SystemMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站内消息：管理员定向发送，接收人查看未读/全部并标记已读。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/system/message")
public class MessageController {

    private final SystemMessageMapper messageMapper;

    /** 我的未读消息 */
    @GetMapping("/my-unread")
    public CommonResult<List<SystemMessageDO>> myUnread() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return CommonResult.success(messageMapper.selectList(new LambdaQueryWrapper<SystemMessageDO>()
                .eq(SystemMessageDO::getUserId, userId)
                .eq(SystemMessageDO::getReadFlag, false)
                .orderByDesc(SystemMessageDO::getId)));
    }

    /** 我的未读数量（顶栏铃铛轮询用） */
    @GetMapping("/my-unread-count")
    public CommonResult<Long> myUnreadCount() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return CommonResult.success(messageMapper.selectCount(new LambdaQueryWrapper<SystemMessageDO>()
                .eq(SystemMessageDO::getUserId, userId)
                .eq(SystemMessageDO::getReadFlag, false)));
    }

    /** 标记单条已读 */
    @PostMapping("/read")
    public CommonResult<Boolean> read(@RequestParam("id") Long id) {
        messageMapper.update(null, new LambdaUpdateWrapper<SystemMessageDO>()
                .eq(SystemMessageDO::getId, id)
                .eq(SystemMessageDO::getUserId, SecurityFrameworkUtils.getLoginUserId())
                .set(SystemMessageDO::getReadFlag, true));
        return CommonResult.success(true);
    }

    /** 全部标记已读 */
    @PostMapping("/read-all")
    public CommonResult<Boolean> readAll() {
        messageMapper.update(null, new LambdaUpdateWrapper<SystemMessageDO>()
                .eq(SystemMessageDO::getUserId, SecurityFrameworkUtils.getLoginUserId())
                .eq(SystemMessageDO::getReadFlag, false)
                .set(SystemMessageDO::getReadFlag, true));
        return CommonResult.success(true);
    }

    /** 发送定向消息（管理员） */
    @PostMapping("/send")
    @PreAuthorize("@ss.hasPermission('infra:message:send')")
    public CommonResult<Long> send(@RequestParam("userId") Long userId,
                                   @RequestParam("title") @NotBlank String title,
                                   @RequestParam(value = "content", required = false) String content) {
        SystemMessageDO message = new SystemMessageDO();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setReadFlag(false);
        message.setCreator(SecurityFrameworkUtils.getLoginUser() != null
                ? SecurityFrameworkUtils.getLoginUser().getUsername() : "");
        messageMapper.insert(message);
        return CommonResult.success(message.getId());
    }
}
