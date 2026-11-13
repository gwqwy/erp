package cn.tiangong.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM 单聊消息。
 */
@Data
@TableName("im_message")
public class ImMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long senderId;

    private Long receiverId;

    private String content;

    private String msgType;

    private Boolean readFlag;

    private LocalDateTime createTime;
}
