package cn.tiangong.module.bpm.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bpm.dal.dataobject.BpmApprovalConfigDO;
import cn.tiangong.module.bpm.dal.dataobject.BpmApprovalTaskDO;

import java.util.List;

public interface ApprovalService {

    /** 提交审批：按配置生成全部级次任务。未配置审批流返回 false（调用方自行直接审核） */
    boolean submit(String bizType, String chatterBizType, Long bizId, String bizNo);

    /** 通过当前级次任务；全部级次通过后发布 ApprovalCompletedEvent */
    void approve(Long taskId, String comment);

    /** 驳回：当前任务驳回、其余待审任务作废，发布 ApprovalRejectedEvent */
    void reject(Long taskId, String comment);

    /** 我的待审批任务 */
    List<BpmApprovalTaskDO> myPendingTasks(Long userId);

    PageResult<BpmApprovalTaskDO> getTaskPage(PageParam pageParam, String bizType, Long approverUserId, Integer status);

    boolean hasApprovalFlow(String bizType);

    // ===== 审批配置 CRUD =====

    PageResult<BpmApprovalConfigDO> getConfigPage(PageParam pageParam, String bizType);

    Long createConfig(String bizType, Integer level, Long approverUserId);

    void updateConfig(Long id, Integer level, Long approverUserId, Integer enabled);

    void deleteConfig(Long id);
}
