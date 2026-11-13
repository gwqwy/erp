package cn.tiangong.framework.biz.common.chatter.controller;

import cn.tiangong.framework.biz.common.chatter.dal.dataobject.BizChatterDO;
import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chatter 接口：登录用户可读、可评论（写入轨迹由业务 service 内部完成）。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/common/chatter")
public class ChatterController {

    private final ChatterService chatterService;

    @GetMapping("/list")
    public CommonResult<List<BizChatterDO>> list(@RequestParam("bizType") String bizType,
                                                 @RequestParam("bizId") Long bizId) {
        return CommonResult.success(chatterService.list(bizType, bizId));
    }

    @PostMapping("/comment")
    public CommonResult<Long> comment(@Valid @RequestBody CommentReqVO reqVO) {
        return CommonResult.success(chatterService.comment(reqVO.getBizType(), reqVO.getBizId(), reqVO.getContent()));
    }

    @Data
    public static class CommentReqVO {
        @NotBlank(message = "bizType 不能为空")
        private String bizType;
        @NotNull(message = "bizId 不能为空")
        private Long bizId;
        @NotBlank(message = "评论内容不能为空")
        private String content;
    }
}
