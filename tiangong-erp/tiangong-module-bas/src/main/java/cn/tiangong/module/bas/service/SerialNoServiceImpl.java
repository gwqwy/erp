package cn.tiangong.module.bas.service;

import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.dal.dataobject.BasSerialRuleDO;
import cn.tiangong.module.bas.dal.mysql.BasSerialRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 单号规则引擎实现：Redis INCR + 日期隔离 Key。
 * Key: tiangong:serial:{bizType}:{datePart}；TTL 90 天兜底，重置语义由日期部分自然实现。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SerialNoServiceImpl implements SerialNoService {

    private static final String KEY_PREFIX = "tiangong:serial:";
    private static final Duration KEY_TTL = Duration.ofDays(90);

    private final BasSerialRuleMapper ruleMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String nextNo(String bizType) {
        BasSerialRuleDO rule = requireRule(bizType);
        String datePart = formatDatePart(rule);
        try {
            String redisKey = KEY_PREFIX + bizType + ":" + datePart;
            Long seq = redisTemplate.opsForValue().increment(redisKey);
            if (seq != null && seq == 1) {
                // key 新建：要么真首单，要么 Redis 重启后序列回退（内存无持久化）。
                // 跳号到 5000 基线，避免与历史单据撞唯一键（唯一性优先于连续性）。
                redisTemplate.opsForValue().set(redisKey, "9000", KEY_TTL);
                seq = 9001L;
            }
            return rule.getPrefix() + datePart + String.format("%0" + rule.getSeqLength() + "d", seq);
        } catch (Exception e) {
            log.error("[nextNo] Redis 取号失败, bizType={}", bizType, e);
            throw of(2_000, 1, "单号生成失败，请检查 Redis 服务");
        }
    }

    @Override
    public String previewNo(String bizType) {
        BasSerialRuleDO rule = requireRule(bizType);
        String datePart = formatDatePart(rule);
        return rule.getPrefix() + datePart + String.format("%0" + rule.getSeqLength() + "d", 1L);
    }

    private BasSerialRuleDO requireRule(String bizType) {
        BasSerialRuleDO rule = ruleMapper.selectByBizType(bizType);
        if (rule == null) {
            throw of(2_000, 2, "未配置单号规则: " + bizType);
        }
        return rule;
    }

    private String formatDatePart(BasSerialRuleDO rule) {
        if (rule.getDateFormat() == null || rule.getDateFormat().isBlank()
                || "none".equals(rule.getResetType())) {
            return "";
        }
        return LocalDate.now().format(DateTimeFormatter.ofPattern(rule.getDateFormat()));
    }

    @Override
    public PageResult<BasSerialRuleDO> getRulePage(PageParam pageParam, String bizType) {
        LambdaQueryWrapper<BasSerialRuleDO> wrapper = new LambdaQueryWrapper<BasSerialRuleDO>()
                .like(bizType != null && !bizType.isBlank(), BasSerialRuleDO::getBizType, bizType)
                .orderByAsc(BasSerialRuleDO::getBizType);
        return ruleMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<BasSerialRuleDO> getRuleList() {
        return ruleMapper.selectList(new LambdaQueryWrapper<BasSerialRuleDO>()
                .orderByAsc(BasSerialRuleDO::getBizType));
    }

    @Override
    public Long createRule(String bizType, String prefix, String dateFormat, Integer seqLength,
                           String resetType, String remark) {
        if (ruleMapper.selectByBizType(bizType) != null) {
            throw of(2_000, 3, "该业务类型的单号规则已存在");
        }
        BasSerialRuleDO rule = new BasSerialRuleDO();
        rule.setBizType(bizType);
        rule.setPrefix(prefix);
        rule.setDateFormat(dateFormat != null ? dateFormat : "yyyyMM");
        rule.setSeqLength(seqLength != null ? seqLength : 4);
        rule.setResetType(resetType != null ? resetType : "month");
        rule.setRemark(remark);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public void updateRule(Long id, String prefix, String dateFormat, Integer seqLength,
                           String resetType, String remark) {
        BasSerialRuleDO rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw of(2_000, 4, "单号规则不存在");
        }
        rule.setPrefix(prefix != null ? prefix : rule.getPrefix());
        rule.setDateFormat(dateFormat != null ? dateFormat : rule.getDateFormat());
        rule.setSeqLength(seqLength != null ? seqLength : rule.getSeqLength());
        rule.setResetType(resetType != null ? resetType : rule.getResetType());
        rule.setRemark(remark != null ? remark : rule.getRemark());
        ruleMapper.updateById(rule);
    }

    @Override
    public void deleteRule(Long id) {
        if (ruleMapper.selectById(id) == null) {
            throw of(2_000, 4, "单号规则不存在");
        }
        ruleMapper.deleteById(id);
    }
}
