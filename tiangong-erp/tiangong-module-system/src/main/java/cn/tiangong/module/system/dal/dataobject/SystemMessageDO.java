package cn.tiangong.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息：定向发送给指定用户。readFlag false 未读 / true 已读。
 */
@Data
@TableName("system_message")
public class SystemMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private Boolean readFlag;

    private String creator;

    private LocalDateTime createTime;
}
