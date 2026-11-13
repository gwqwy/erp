package cn.tiangong.module.im.service;

import cn.tiangong.module.im.dal.dataobject.ImMessageDO;
import cn.tiangong.module.im.dal.dataobject.ImSessionDO;
import cn.tiangong.module.im.dal.mysql.ImMessageMapper;
import cn.tiangong.module.im.dal.mysql.ImSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * IM 查询服务：会话列表、历史消息。
 */
@RequiredArgsConstructor
@Service
public class ImChatService {

    private final ImMessageMapper messageMapper;
    private final ImSessionMapper sessionMapper;

    /** 我的会话列表（含对方 id 与最后一条消息） */
    public List<ImSessionDO> mySessions(Long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ImSessionDO>()
                .eq(ImSessionDO::getUserA, userId)
                .or()
                .eq(ImSessionDO::getUserB, userId)
                .orderByDesc(ImSessionDO::getLastTime));
    }

    /** 与某人的历史消息（升序） */
    public List<ImMessageDO> history(String sessionKey) {
        return messageMapper.selectList(new LambdaQueryWrapper<ImMessageDO>()
                .eq(ImMessageDO::getSessionId, sessionKey)
                .orderByAsc(ImMessageDO::getId));
    }
}
