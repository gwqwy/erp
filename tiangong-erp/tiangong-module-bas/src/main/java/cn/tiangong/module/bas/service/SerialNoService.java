package cn.tiangong.module.bas.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.dal.dataobject.BasSerialRuleDO;

import java.util.List;

/**
 * 单号规则引擎（《02-总体架构》§4.6）：
 * 号段格式 {prefix}{datePart}{seq}，Redis INCR 生成序列，日期隔离实现按天/月/年重置。
 */
public interface SerialNoService {

    /** 取下一个单号（消耗序列）。规则不存在或 Redis 不可用时抛业务异常 */
    String nextNo(String bizType);

    /** 预览下一个单号（不消耗序列，返回 prefix+datePart+"0001" 样式） */
    String previewNo(String bizType);

    PageResult<BasSerialRuleDO> getRulePage(PageParam pageParam, String bizType);

    List<BasSerialRuleDO> getRuleList();

    Long createRule(String bizType, String prefix, String dateFormat, Integer seqLength, String resetType, String remark);

    void updateRule(Long id, String prefix, String dateFormat, Integer seqLength, String resetType, String remark);

    void deleteRule(Long id);
}
