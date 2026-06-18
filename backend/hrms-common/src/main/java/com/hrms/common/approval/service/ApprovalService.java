package com.hrms.common.approval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.common.approval.entity.ApprovalDefinition;
import com.hrms.common.approval.entity.ApprovalHistory;
import com.hrms.common.approval.entity.ApprovalInstance;
import com.hrms.common.approval.entity.ApprovalTask;
import com.hrms.common.approval.event.ApprovalCompletedEvent;
import com.hrms.common.approval.mapper.ApprovalDefinitionMapper;
import com.hrms.common.approval.mapper.ApprovalHistoryMapper;
import com.hrms.common.approval.mapper.ApprovalInstanceMapper;
import com.hrms.common.approval.mapper.ApprovalTaskMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.api.BizCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Lightweight approval engine: start, approve, reject, revoke, query.
 *
 * <p>Nodes are stored as JSON in approval_definition and parsed on the fly.
 * State machine: PENDING -> APPROVED / REJECTED / REVOKED / SUSPENDED.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalDefinitionMapper definitionMapper;
    private final ApprovalInstanceMapper instanceMapper;
    private final ApprovalTaskMapper taskMapper;
    private final ApprovalHistoryMapper historyMapper;
    private final ApproverResolver approverResolver;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Start a new approval workflow instance from a definition code.
     */
    @Transactional
    public Long start(String definitionCode, String businessKey, String businessType,
                      String payload, Long applicantId) {
        // Find and validate definition
        ApprovalDefinition def = definitionMapper.selectOne(
                new LambdaQueryWrapper<ApprovalDefinition>()
                        .eq(ApprovalDefinition::getCode, definitionCode)
                        .eq(ApprovalDefinition::getEnabled, true));
        if (def == null) {
            throw new BizException(BizCode.BAD_REQUEST, "审批定义不存在或已禁用: " + definitionCode);
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // Create instance
        ApprovalInstance instance = new ApprovalInstance();
        instance.setDefinitionId(def.getId());
        instance.setBusinessType(businessType != null ? businessType : def.getBusinessType());
        instance.setBusinessKey(businessKey);
        instance.setApplicantId(applicantId);
        instance.setCurrentNodeSeq(1);
        instance.setStatus("PENDING");
        instance.setPayload(payload);
        instance.setStartedAt(now);
        instanceMapper.insert(instance);

        // Record START history
        ApprovalHistory startHistory = new ApprovalHistory();
        startHistory.setInstanceId(instance.getId());
        startHistory.setNodeSeq(0);
        startHistory.setActorId(applicantId);
        startHistory.setAction("START");
        startHistory.setActedAt(now);
        historyMapper.insert(startHistory);

        // Create first task
        createTaskForNode(instance, def, 1, applicantId, now);

        log.info("Approval started: instance={}, def={}, applicant={}", instance.getId(), definitionCode, applicantId);
        return instance.getId();
    }

    /**
     * Approve a pending task and advance to next node or complete the instance.
     */
    @Transactional
    public void approve(Long taskId, Long approverId, String comment) {
        ApprovalTask task = getAndValidateTask(taskId, approverId);
        validatePendingStatus(task.getStatus());

        LocalDateTime now = LocalDateTime.now(clock);

        // Mark task approved
        task.setStatus("APPROVED");
        task.setActedAt(now);
        taskMapper.updateById(task);

        ApprovalInstance instance = instanceMapper.selectById(task.getInstanceId());
        ApprovalDefinition def = definitionMapper.selectById(instance.getDefinitionId());
        List<Map<String, Object>> nodes = parseNodes(def.getNodes());
        int totalNodes = nodes.size();

        // Record history
        recordHistory(instance.getId(), task.getId(), task.getNodeSeq(), approverId, "APPROVE", comment, now);

        if (task.getNodeSeq() < totalNodes) {
            // Advance to next node
            int nextSeq = task.getNodeSeq() + 1;
            instance.setCurrentNodeSeq(nextSeq);
            instanceMapper.updateById(instance);
            createTaskForNode(instance, def, nextSeq, applicantIdOf(instance), now);
            log.info("Task {} approved, advancing to node {}", taskId, nextSeq);
        } else {
            // All nodes approved — complete
            instance.setStatus("APPROVED");
            instance.setFinishedAt(now);
            instanceMapper.updateById(instance);
            eventPublisher.publishEvent(new ApprovalCompletedEvent(
                    instance.getBusinessType(), instance.getBusinessKey(), "APPROVED"));
            log.info("Approval instance {} fully approved", instance.getId());
        }
    }

    /**
     * Reject a pending task, rejecting the entire instance.
     */
    @Transactional
    public void reject(Long taskId, Long approverId, String comment) {
        ApprovalTask task = getAndValidateTask(taskId, approverId);
        validatePendingStatus(task.getStatus());

        LocalDateTime now = LocalDateTime.now(clock);

        task.setStatus("REJECTED");
        task.setActedAt(now);
        taskMapper.updateById(task);

        ApprovalInstance instance = instanceMapper.selectById(task.getInstanceId());
        instance.setStatus("REJECTED");
        instance.setFinishedAt(now);
        instanceMapper.updateById(instance);

        recordHistory(instance.getId(), task.getId(), task.getNodeSeq(), approverId, "REJECT", comment, now);

        eventPublisher.publishEvent(new ApprovalCompletedEvent(
                instance.getBusinessType(), instance.getBusinessKey(), "REJECTED"));
        log.info("Task {} rejected by {}", taskId, approverId);
    }

    /**
     * Revoke a previously approved task, rolling back to the previous node.
     * Only the original approver of the task can revoke.
     */
    @Transactional
    public void revoke(Long taskId, Long approverId) {
        ApprovalTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(BizCode.BAD_REQUEST, "审批任务不存在: " + taskId);
        }
        if (!"APPROVED".equals(task.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "只能撤销已通过的任务");
        }
        if (!task.getAssigneeId().equals(approverId)) {
            throw new BizException(BizCode.FORBIDDEN, "只能撤销自己审批通过的任务");
        }

        ApprovalInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (!"PENDING".equals(instance.getStatus()) && !"APPROVED".equals(instance.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "当前实例状态不允许撤销");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // Revert the current task
        task.setStatus("REVOKED");
        task.setActedAt(now);
        taskMapper.updateById(task);

        // If this was the last node and instance was completed, revert to previous node
        ApprovalDefinition def = definitionMapper.selectById(instance.getDefinitionId());
        List<Map<String, Object>> nodes = parseNodes(def.getNodes());

        if (task.getNodeSeq() > 1) {
            // Roll back: revoke next node's tasks, reset instance to previous node
            int prevSeq = task.getNodeSeq() - 1;
            // Mark any tasks at current node or beyond as revoked
            revokeSubsequentTasks(instance.getId(), task.getNodeSeq());
            instance.setCurrentNodeSeq(prevSeq);
            instance.setStatus("PENDING");
            instance.setFinishedAt(null);
            instanceMapper.updateById(instance);
            createTaskForNode(instance, def, prevSeq, applicantIdOf(instance), now);
        } else {
            // First node revoked — cannot go further back
            instance.setStatus("REVOKED");
            instance.setFinishedAt(now);
            instanceMapper.updateById(instance);
        }

        recordHistory(instance.getId(), task.getId(), task.getNodeSeq(), approverId, "REVOKE", null, now);
        log.info("Task {} revoked by {}", taskId, approverId);
    }

    /**
     * Get pending tasks assigned to the given user.
     */
    public List<ApprovalTask> getTodoList(Long userId) {
        return taskMapper.selectList(
                new LambdaQueryWrapper<ApprovalTask>()
                        .eq(ApprovalTask::getAssigneeId, userId)
                        .eq(ApprovalTask::getStatus, "PENDING")
                        .orderByDesc(ApprovalTask::getCreatedAt));
    }

    /**
     * Get the full history of an approval instance.
     */
    public List<ApprovalHistory> getHistory(Long instanceId) {
        return historyMapper.selectList(
                new LambdaQueryWrapper<ApprovalHistory>()
                        .eq(ApprovalHistory::getInstanceId, instanceId)
                        .orderByAsc(ApprovalHistory::getCreatedAt));
    }

    // -- internals -------------------------------------------------------

    private ApprovalTask getAndValidateTask(Long taskId, Long approverId) {
        ApprovalTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(BizCode.BAD_REQUEST, "审批任务不存在: " + taskId);
        }
        if (!task.getAssigneeId().equals(approverId)) {
            throw new BizException(BizCode.FORBIDDEN, "无权操作此审批任务");
        }
        return task;
    }

    private void validatePendingStatus(String status) {
        if (!"PENDING".equals(status)) {
            throw new BizException(BizCode.BAD_REQUEST, "当前任务状态不允许此操作: " + status);
        }
    }

    private void createTaskForNode(ApprovalInstance instance, ApprovalDefinition def,
                                   int nodeSeq, Long applicantId, LocalDateTime now) {
        List<Map<String, Object>> nodes = parseNodes(def.getNodes());
        Map<String, Object> node = nodes.get(nodeSeq - 1);
        String approverRule = (String) node.get("approverRule");

        Long assigneeId = approverResolver.resolve(approverRule, applicantId, instance);

        ApprovalTask newTask = new ApprovalTask();
        newTask.setInstanceId(instance.getId());
        newTask.setNodeSeq(nodeSeq);
        newTask.setAssigneeId(assigneeId);
        newTask.setAssignedAt(now);

        if (assigneeId == null) {
            // No eligible approver — suspend the instance
            newTask.setStatus("PENDING");
            taskMapper.insert(newTask);
            instance.setStatus("SUSPENDED");
            instanceMapper.updateById(instance);
            recordHistory(instance.getId(), newTask.getId(), nodeSeq, applicantId, "SUSPEND",
                    "无可匹配的审批人", now);
            log.warn("Instance {} suspended: no approver for rule {}", instance.getId(), approverRule);
        } else {
            newTask.setStatus("PENDING");
            taskMapper.insert(newTask);
            log.info("Created task for instance={}, node={}, assignee={}", instance.getId(), nodeSeq, assigneeId);
        }
    }

    private void recordHistory(Long instanceId, Long taskId, int nodeSeq, Long actorId,
                               String action, String comment, LocalDateTime now) {
        ApprovalHistory history = new ApprovalHistory();
        history.setInstanceId(instanceId);
        history.setTaskId(taskId);
        history.setNodeSeq(nodeSeq);
        history.setActorId(actorId);
        history.setAction(action);
        history.setComment(comment);
        history.setActedAt(now);
        historyMapper.insert(history);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseNodes(String nodesJson) {
        try {
            return objectMapper.readValue(nodesJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new BizException(BizCode.INTERNAL_ERROR, "审批节点 JSON 解析失败");
        }
    }

    private void revokeSubsequentTasks(Long instanceId, int fromSeq) {
        List<ApprovalTask> subsequent = taskMapper.selectList(
                new LambdaQueryWrapper<ApprovalTask>()
                        .eq(ApprovalTask::getInstanceId, instanceId)
                        .ge(ApprovalTask::getNodeSeq, fromSeq));
        for (ApprovalTask t : subsequent) {
            if ("APPROVED".equals(t.getStatus()) || "PENDING".equals(t.getStatus())) {
                t.setStatus("REVOKED");
                t.setActedAt(LocalDateTime.now(clock));
                taskMapper.updateById(t);
            }
        }
    }

    private Long applicantIdOf(ApprovalInstance instance) {
        return instance.getApplicantId();
    }
}
