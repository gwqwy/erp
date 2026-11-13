package cn.tiangong.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM 会话（冗余最后一条消息，会话列表用）。
 */
@Data
@TableName("im_session")
public class ImSessionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionKey;

    private Long userA;

    private Long userB;

    private String lastContent;

    private LocalDateTime lastTime;

    private LocalDateTime updateTime;
}
