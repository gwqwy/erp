package cn.tiangong.framework.biz.common.chatter.service;

import cn.tiangong.framework.biz.common.chatter.dal.dataobject.BizChatterDO;
import cn.tiangong.framework.biz.common.chatter.dal.mysql.BizChatterMapper;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Chatter 服务：任意模块注入即可为其单据记录动态。
 */
@Service
@RequiredArgsConstructor
public class ChatterService {

    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_TRACE = "system_trace";

    private final BizChatterMapper chatterMapper;

    /** 记录系统轨迹（审核、作废、过账等关键动作） */
    @SneakyThrows
    public void trace(String bizType, Long bizId, String summary, Map<String, Object> extra) {
        BizChatterDO chatter = new BizChatterDO();
        chatter.setBizType(bizType);
        chatter.setBizId(bizId);
        chatter.setMsgType(TYPE_TRACE);
        chatter.setContent(summary);
        chatter.setUserId(SecurityFrameworkUtils.getLoginUserId());
        if (extra != null && !extra.isEmpty()) {
            chatter.setExtra(new ObjectMapper().writeValueAsString(extra));
        }
        chatterMapper.insert(chatter);
    }

    /** 发表评论 */
    public Long comment(String bizType, Long bizId, String content) {
        BizChatterDO chatter = new BizChatterDO();
        chatter.setBizType(bizType);
        chatter.setBizId(bizId);
        chatter.setMsgType(TYPE_COMMENT);
        chatter.setContent(content);
        chatter.setUserId(SecurityFrameworkUtils.getLoginUserId());
        chatterMapper.insert(chatter);
        return chatter.getId();
    }

    /** 按时间正序读取单据动态 */
    public List<BizChatterDO> list(String bizType, Long bizId) {
        return chatterMapper.selectList(new LambdaQueryWrapper<BizChatterDO>()
                .eq(BizChatterDO::getBizType, bizType)
                .eq(BizChatterDO::getBizId, bizId)
                .orderByAsc(BizChatterDO::getId));
    }
}
