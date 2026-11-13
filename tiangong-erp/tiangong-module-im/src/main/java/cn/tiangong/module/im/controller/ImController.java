package cn.tiangong.module.im.controller.admin;

import cn.tiangong.framework.security.core.LoginUser;
import cn.tiangong.framework.security.core.TokenService;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.im.dal.dataobject.ImMessageDO;
import cn.tiangong.module.im.dal.dataobject.ImSessionDO;
import cn.tiangong.module.im.service.ImChatService;
import cn.tiangong.module.im.websocket.ImPushService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/im")
public class ImController {

    private final ImChatService chatService;
    private final ImPushService pushService;
    private final TokenService tokenService;

    @GetMapping("/sessions")
    @PreAuthorize("@ss.hasPermission('im:query')")
    public CommonResult<List<ImSessionDO>> mySessions(
            @RequestParam(value = "token", required = false) String token) {
        Long me = currentUserId();
        return CommonResult.success(chatService.mySessions(me));
    }

    @GetMapping("/history")
    @PreAuthorize("@ss.hasPermission('im:query')")
    public CommonResult<List<ImMessageDO>> history(@RequestParam("sessionKey") String sessionKey) {
        return CommonResult.success(chatService.history(sessionKey));
    }

    /** REST 发送（与 WebSocket 发送等效，消息落库 + 在线推送） */
    @PostMapping("/send")
    @PreAuthorize("@ss.hasPermission('im:chat')")
    public CommonResult<ImMessageDO> send(@RequestParam("receiverId") @NotNull Long receiverId,
                                          @RequestParam("content") @NotBlank String content) {
        Long me = currentUserId();
        return CommonResult.success(pushService.sendChat(me, receiverId, content));
    }

    private Long currentUserId() {
        LoginUser user = cn.tiangong.framework.security.util.SecurityFrameworkUtils.getLoginUser();
        return user != null ? user.getId() : null;
    }
}
