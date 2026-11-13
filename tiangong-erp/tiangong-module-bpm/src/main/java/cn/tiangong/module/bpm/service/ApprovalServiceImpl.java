package cn.tiangong.module.bpm.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bpm.api.ApprovalCompletedEvent;
import cn.tiangong.module.bpm.api.ApprovalRejectedEvent;
import cn.tiangong.module.bpm.dal.dataobject.BpmApprovalConfigDO;
import cn.tiangong.module.bpm.dal.dataobject.BpmApprovalTaskDO;
import cn.tiangong.module.bpm.dal.mysql.BpmApprovalConfigMapper;
import cn.tiangong.module.bpm.dal.mysql.BpmApprovalTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class ApprovalServiceImpl implements ApprovalService {

    public static final int TASK_PENDING = 1;
    public static final int TASK_PASS = 2;
    public static final int TASK_REJECT = 3;
    public static final int TASK_CANCELLED = 4;

    private final BpmApprovalConfigMapper configMapper;
    private final BpmApprovalTaskMapper taskMapper;
    private final ChatterService chatterService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(String bizType, String chatterBizType, Long bizId, String bizNo) {
        List<BpmApprovalConfigDO> configs = configMapper.selectList(
                new LambdaQueryWrapper<BpmApprovalConfigDO>()
                        .eq(BpmApprovalConfigDO::getBizType, bizType)
                        .eq(BpmApprovalConfigDO::getEnabled, 1)
                        .orderByAsc(BpmApprovalConfigDO::getLevel));
        if (configs.isEmpty()) {
            return false;
        }
        // 清理同单历史残留的待审任务（脚本重跑/重复提交场景），避免堵塞级次流转
        taskMapper.update(null, new LambdaUpdateWrapper<BpmApprovalTaskDO>()
                .eq(BpmApprovalTaskDO::getBizType, bizType)
                .eq(BpmApprovalTaskDO::getBizId, bizId)
                .eq(BpmApprovalTaskDO::getStatus, TASK_PENDING)
                .set(BpmApprovalTaskDO::getStatus, TASK_CANCELLED));
        for (BpmApprovalConfigDO config : configs) {
            BpmApprovalTaskDO task = new BpmApprovalTaskDO();
            task.setBizType(bizType);
            task.setBizId(bizId);
            task.setBizNo(bizNo);
            task.setLevel(config.getLevel());
            task.setApproverUserId(config.getApproverUserId());
            task.setStatus(TASK_PENDING);
            taskMapper.insert(task);
        }
        chatterService.trace(chatterBizType, bizId,
                String.format("提交审批（%d 级），当前待第 1 级审批人处理", configs.size()), null);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long taskId, String comment) {
        BpmApprovalTaskDO task = getTask(taskId);
        requirePending(task);
        requireCurrentApprover(task);
        task.setStatus(TASK_PASS);
        task.setComment(comment);
        taskMapper.updateById(task);

        List<BpmApprovalTaskDO> pending = pendingTasks(task.getBizType(), task.getBizId());
        if (pending.isEmpty()) {
            // 全部级次通过 → 事件通知业务模块执行审核过账
            eventPublisher.publishEvent(new ApprovalCompletedEvent(
                    task.getBizType(), task.getBizType(), task.getBizId(), task.getBizNo()));
        } else {
            int nextLevel = pending.stream().mapToInt(BpmApprovalTaskDO::getLevel).min().orElse(0);
            chatterService.trace(task.getBizType(), task.getBizId(),
                    String.format("第 %d 级审批通过，流转至第 %d 级", task.getLevel(), nextLevel), null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, String comment) {
        BpmApprovalTaskDO task = getTask(taskId);
        requirePending(task);
        requireCurrentApprover(task);
        task.setStatus(TASK_REJECT);
        task.setComment(comment);
        taskMapper.updateById(task);
        // 其余待审任务作废
        taskMapper.update(null, new LambdaUpdateWrapper<BpmApprovalTaskDO>()
                .eq(BpmApprovalTaskDO::getBizType, task.getBizType())
                .eq(BpmApprovalTaskDO::getBizId, task.getBizId())
                .eq(BpmApprovalTaskDO::getStatus, TASK_PENDING)
                .set(BpmApprovalTaskDO::getStatus, TASK_CANCELLED));
        chatterService.trace(task.getBizType(), task.getBizId(),
                String.format("第 %d 级审批驳回：%s", task.getLevel(), comment == null ? "" : comment), null);
        eventPublisher.publishEvent(new ApprovalRejectedEvent(
                task.getBizType(), task.getBizType(), task.getBizId(), task.getBizNo()));
    }

    @Override
    public List<BpmApprovalTaskDO> myPendingTasks(Long userId) {
        return taskMapper.selectList(new LambdaQueryWrapper<BpmApprovalTaskDO>()
                .eq(BpmApprovalTaskDO::getApproverUserId, userId)
                .eq(BpmApprovalTaskDO::getStatus, TASK_PENDING)
                .orderByDesc(BpmApprovalTaskDO::getId));
    }

    @Override
    public PageResult<BpmApprovalTaskDO> getTaskPage(PageParam pageParam, String bizType,
                                                     Long approverUserId, Integer status) {
        LambdaQueryWrapper<BpmApprovalTaskDO> wrapper = new LambdaQueryWrapper<BpmApprovalTaskDO>()
                .eq(bizType != null && !bizType.isBlank(), BpmApprovalTaskDO::getBizType, bizType)
                .eq(approverUserId != null, BpmApprovalTaskDO::getApproverUserId, approverUserId)
                .eq(status != null, BpmApprovalTaskDO::getStatus, status)
                .orderByDesc(BpmApprovalTaskDO::getId);
        return taskMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public boolean hasApprovalFlow(String bizType) {
        return configMapper.selectCount(new LambdaQueryWrapper<BpmApprovalConfigDO>()
                .eq(BpmApprovalConfigDO::getBizType, bizType)
                .eq(BpmApprovalConfigDO::getEnabled, 1)) > 0;
    }

    @Override
    public PageResult<BpmApprovalConfigDO> getConfigPage(PageParam pageParam, String bizType) {
        LambdaQueryWrapper<BpmApprovalConfigDO> wrapper = new LambdaQueryWrapper<BpmApprovalConfigDO>()
                .eq(bizType != null && !bizType.isBlank(), BpmApprovalConfigDO::getBizType, bizType)
                .orderByAsc(BpmApprovalConfigDO::getBizType)
                .orderByAsc(BpmApprovalConfigDO::getLevel);
        return configMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Long createConfig(String bizType, Integer level, Long approverUserId) {
        if (bizType == null || bizType.isBlank() || level == null || approverUserId == null) {
            throw of(12_00, 1, "业务类型/级次/审批人不能为空");
        }
        Long dup = configMapper.selectCount(new LambdaQueryWrapper<BpmApprovalConfigDO>()
                .eq(BpmApprovalConfigDO::getBizType, bizType)
                .eq(BpmApprovalConfigDO::getLevel, level));
        if (dup > 0) {
            throw of(12_00, 3, String.format("该业务类型第 %d 级审批人已配置，请直接修改", level));
        }
        BpmApprovalConfigDO config = new BpmApprovalConfigDO();
        config.setBizType(bizType);
        config.setLevel(level);
        config.setApproverUserId(approverUserId);
        config.setEnabled(1);
        configMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateConfig(Long id, Integer level, Long approverUserId, Integer enabled) {
        BpmApprovalConfigDO config = configMapper.selectById(id);
        if (config == null) {
            throw of(12_00, 2, "审批配置不存在");
        }
        if (level != null) config.setLevel(level);
        if (approverUserId != null) config.setApproverUserId(approverUserId);
        if (enabled != null) config.setEnabled(enabled);
        configMapper.updateById(config);
    }

    @Override
    public void deleteConfig(Long id) {
        if (configMapper.selectById(id) == null) {
            throw of(12_00, 2, "审批配置不存在");
        }
        configMapper.deleteById(id);
    }

    private void requirePending(BpmApprovalTaskDO task) {
        if (task.getStatus() != TASK_PENDING) {
            throw new ServiceException(12_01_0001, "该任务不是待审批状态");
        }
    }

    private void requireCurrentApprover(BpmApprovalTaskDO task) {
        List<BpmApprovalTaskDO> pending = pendingTasks(task.getBizType(), task.getBizId());
        int lowest = pending.stream().mapToInt(BpmApprovalTaskDO::getLevel).min().orElse(0);
        if (task.getLevel() != lowest) {
            throw of(12_01, 2, String.format("当前应先处理第 %d 级审批", lowest));
        }
        Long loginUser = SecurityFrameworkUtils.getLoginUserId();
        if (!String.valueOf(task.getApproverUserId()).equals(String.valueOf(loginUser))) {
            throw of(12_01, 3, "该级次审批人不是当前用户");
        }
    }

    private List<BpmApprovalTaskDO> pendingTasks(String bizType, Long bizId) {
        return taskMapper.selectList(new LambdaQueryWrapper<BpmApprovalTaskDO>()
                .eq(BpmApprovalTaskDO::getBizType, bizType)
                .eq(BpmApprovalTaskDO::getBizId, bizId)
                .eq(BpmApprovalTaskDO::getStatus, TASK_PENDING));
    }

    private BpmApprovalTaskDO getTask(Long id) {
        BpmApprovalTaskDO task = taskMapper.selectById(id);
        if (task == null) {
            throw of(12_01, 4, "审批任务不存在");
        }
        return task;
    }
}
