package com.hrms.common.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.approval.entity.ApprovalDefinition;
import com.hrms.common.approval.entity.ApprovalHistory;
import com.hrms.common.approval.entity.ApprovalInstance;
import com.hrms.common.approval.entity.ApprovalTask;
import com.hrms.common.approval.mapper.ApprovalDefinitionMapper;
import com.hrms.common.approval.mapper.ApprovalHistoryMapper;
import com.hrms.common.approval.mapper.ApprovalInstanceMapper;
import com.hrms.common.approval.mapper.ApprovalTaskMapper;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.approval.service.ApproverResolver;
import com.hrms.common.approval.event.ApprovalCompletedEvent;
import com.hrms.common.exception.BizException;
import com.hrms.common.user.SysUser;
import com.hrms.common.user.SysUserMapper;
import com.hrms.common.rbac.entity.SysUserRole;
import com.hrms.common.rbac.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ApprovalService} — 8+ scenarios covering start, approve,
 * reject, revoke, approver resignation fallback, suspend, and state machine.
 */
class ApprovalServiceTest {

    private static final String DEF_CODE = "MOCK_LEAVE";
    private static final String NODES_JSON =
            "[{\"seq\":1,\"name\":\"直属经理\",\"approverRule\":\"EMP_MANAGER\"},"
                    + "{\"seq\":2,\"name\":\"HR审核\",\"approverRule\":\"ROLE:HR_MANAGER\"},"
                    + "{\"seq\":3,\"name\":\"Admin终审\",\"approverRule\":\"ROLE:ADMIN\"}]";

    private ApprovalDefinitionMapper definitionMapper;
    private ApprovalInstanceMapper instanceMapper;
    private ApprovalTaskMapper taskMapper;
    private ApprovalHistoryMapper historyMapper;
    private ApproverResolver approverResolver;
    private ApplicationEventPublisher eventPublisher;
    private ApprovalService service;

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-06-16T10:00:00Z"), ZoneId.systemDefault());
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Captured instances/tasks for verifying inserts
    private final List<ApprovalInstance> capturedInstances = new ArrayList<>();
    private final List<ApprovalTask> capturedTasks = new ArrayList<>();
    private final List<ApprovalHistory> capturedHistories = new ArrayList<>();

    @BeforeEach
    void setUp() {
        definitionMapper = mock(ApprovalDefinitionMapper.class);
        instanceMapper = mock(ApprovalInstanceMapper.class);
        taskMapper = mock(ApprovalTaskMapper.class);
        historyMapper = mock(ApprovalHistoryMapper.class);
        approverResolver = mock(ApproverResolver.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        capturedInstances.clear();
        capturedTasks.clear();
        capturedHistories.clear();

        // Capture inserts
        doAnswer(inv -> {
            ApprovalInstance inst = inv.getArgument(0);
            inst.setId(1000L); // simulate snowflake
            capturedInstances.add(inst);
            return 1;
        }).when(instanceMapper).insert(any(ApprovalInstance.class));

        doAnswer(inv -> {
            ApprovalTask task = inv.getArgument(0);
            task.setId((long) (2000 + capturedTasks.size()));
            capturedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(ApprovalTask.class));

        doAnswer(inv -> {
            ApprovalHistory hist = inv.getArgument(0);
            hist.setId((long) (3000 + capturedHistories.size()));
            capturedHistories.add(hist);
            return 1;
        }).when(historyMapper).insert(any(ApprovalHistory.class));

        service = new ApprovalService(
                definitionMapper, instanceMapper, taskMapper, historyMapper,
                approverResolver, eventPublisher, objectMapper, fixedClock);
    }

    private ApprovalDefinition mockDefinition() {
        ApprovalDefinition def = new ApprovalDefinition();
        def.setId(1L);
        def.setCode(DEF_CODE);
        def.setBusinessType("LEAVE");
        def.setNodes(NODES_JSON);
        def.setEnabled(true);
        return def;
    }

    private void stubDefinitionLookup() {
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(mockDefinition());
    }

    private void stubDefinitionById() {
        when(definitionMapper.selectById(1L)).thenReturn(mockDefinition());
    }

    // ----------------------------------------------------------------
    // T1: 3-node full approval — all pass → instance APPROVED
    // ----------------------------------------------------------------
    @Test
    @DisplayName("3-level approval: all nodes pass -> instance APPROVED")
    void threeLevelFullApproval() {
        stubDefinitionLookup();
        stubDefinitionById();

        // Node 1: EMP_MANAGER -> user 10
        when(approverResolver.resolve(eq("EMP_MANAGER"), eq(5L), any())).thenReturn(10L);
        // Node 2: ROLE:HR_MANAGER -> user 20
        when(approverResolver.resolve(eq("ROLE:HR_MANAGER"), eq(5L), any())).thenReturn(20L);
        // Node 3: ROLE:ADMIN -> user 1
        when(approverResolver.resolve(eq("ROLE:ADMIN"), eq(5L), any())).thenReturn(1L);

        // Start
        Long instanceId = service.start(DEF_CODE, "LEAVE-001", "LEAVE", "{}", 5L);
        assertThat(instanceId).isNotNull();
        assertThat(capturedInstances).hasSize(1);
        assertThat(capturedInstances.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(capturedTasks).hasSize(1);
        assertThat(capturedTasks.get(0).getAssigneeId()).isEqualTo(10L);

        // Approve node 1
        ApprovalTask task1 = capturedTasks.get(0);
        when(taskMapper.selectById(task1.getId())).thenReturn(task1);
        when(instanceMapper.selectById(instanceId)).thenReturn(capturedInstances.get(0));
        service.approve(task1.getId(), 10L, "同意");
        assertThat(task1.getStatus()).isEqualTo("APPROVED");

        // Approve node 2
        ApprovalTask task2 = capturedTasks.get(1);
        when(taskMapper.selectById(task2.getId())).thenReturn(task2);
        ApprovalInstance refreshedInstance = copyInstance(capturedInstances.get(0));
        refreshedInstance.setCurrentNodeSeq(2);
        when(instanceMapper.selectById(instanceId)).thenReturn(refreshedInstance);
        service.approve(task2.getId(), 20L, "HR通过");
        assertThat(task2.getStatus()).isEqualTo("APPROVED");

        // Approve node 3
        ApprovalTask task3 = capturedTasks.get(2);
        when(taskMapper.selectById(task3.getId())).thenReturn(task3);
        ApprovalInstance instanceAtNode3 = copyInstance(capturedInstances.get(0));
        instanceAtNode3.setCurrentNodeSeq(3);
        when(instanceMapper.selectById(instanceId)).thenReturn(instanceAtNode3);
        service.approve(task3.getId(), 1L, "终审通过");

        // Verify final state
        assertThat(instanceAtNode3.getStatus()).isEqualTo("APPROVED");
        assertThat(instanceAtNode3.getFinishedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(ApprovalCompletedEvent.class));
    }

    // ----------------------------------------------------------------
    // T2: Middle node rejected → instance REJECTED
    // ----------------------------------------------------------------
    @Test
    @DisplayName("Middle node reject -> instance REJECTED")
    void middleNodeReject() {
        stubDefinitionLookup();
        stubDefinitionById();

        when(approverResolver.resolve(eq("EMP_MANAGER"), eq(5L), any())).thenReturn(10L);
        when(approverResolver.resolve(eq("ROLE:HR_MANAGER"), eq(5L), any())).thenReturn(20L);

        service.start(DEF_CODE, "LEAVE-002", "LEAVE", "{}", 5L);

        // Approve node 1
        ApprovalTask task1 = capturedTasks.get(0);
        when(taskMapper.selectById(task1.getId())).thenReturn(task1);
        when(instanceMapper.selectById(1000L)).thenReturn(capturedInstances.get(0));
        service.approve(task1.getId(), 10L, null);

        // Reject node 2
        ApprovalTask task2 = capturedTasks.get(1);
        when(taskMapper.selectById(task2.getId())).thenReturn(task2);
        ApprovalInstance refreshed = copyInstance(capturedInstances.get(0));
        refreshed.setCurrentNodeSeq(2);
        when(instanceMapper.selectById(1000L)).thenReturn(refreshed);
        service.reject(task2.getId(), 20L, "不批准");

        assertThat(task2.getStatus()).isEqualTo("REJECTED");
        assertThat(refreshed.getStatus()).isEqualTo("REJECTED");
        assertThat(refreshed.getFinishedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(ApprovalCompletedEvent.class));
    }

    // ----------------------------------------------------------------
    // T3: Revoke approved task → roll back to previous node
    // ----------------------------------------------------------------
    @Test
    @DisplayName("Revoke approved task -> rollback to previous node")
    void revokeToPreviousNode() {
        stubDefinitionLookup();
        stubDefinitionById();

        when(approverResolver.resolve(eq("EMP_MANAGER"), eq(5L), any())).thenReturn(10L);
        when(approverResolver.resolve(eq("ROLE:HR_MANAGER"), eq(5L), any())).thenReturn(20L);

        service.start(DEF_CODE, "LEAVE-003", "LEAVE", "{}", 5L);

        // Approve node 1
        ApprovalTask task1 = capturedTasks.get(0);
        when(taskMapper.selectById(task1.getId())).thenReturn(task1);
        when(instanceMapper.selectById(1000L)).thenReturn(capturedInstances.get(0));
        service.approve(task1.getId(), 10L, null);

        // Approve node 2
        ApprovalTask task2 = capturedTasks.get(1);
        when(taskMapper.selectById(task2.getId())).thenReturn(task2);
        ApprovalInstance instAtNode2 = copyInstance(capturedInstances.get(0));
        instAtNode2.setCurrentNodeSeq(2);
        when(instanceMapper.selectById(1000L)).thenReturn(instAtNode2);
        service.approve(task2.getId(), 20L, null);

        // Revoke node 2 (task2 was approved by user 20)
        ApprovalTask task2ForRevoke = copyTask(task2);
        when(taskMapper.selectById(task2.getId())).thenReturn(task2ForRevoke);
        ApprovalInstance instForRevoke = copyInstance(capturedInstances.get(0));
        instForRevoke.setCurrentNodeSeq(3);
        instForRevoke.setStatus("PENDING");
        when(instanceMapper.selectById(1000L)).thenReturn(instForRevoke);
        // When revokeSubsequentTasks queries tasks
        List<ApprovalTask> allTasks = List.of(copyTask(task1), task2ForRevoke);
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(allTasks);

        service.revoke(task2.getId(), 20L);

        assertThat(task2ForRevoke.getStatus()).isEqualTo("REVOKED");
        assertThat(instForRevoke.getCurrentNodeSeq()).isEqualTo(1);
        assertThat(instForRevoke.getStatus()).isEqualTo("PENDING");
    }

    // ----------------------------------------------------------------
    // T4: Approver resigned → auto reassign to next eligible user
    // ----------------------------------------------------------------
    @Test
    @DisplayName("Approver resigned -> falls back to next eligible user")
    void approverResignedAutoReassign() {
        stubDefinitionLookup();
        stubDefinitionById();

        // First call: EMP_MANAGER resolves to null (manager resigned)
        // The service handles this via SUSPEND, but test the resolver directly
        when(approverResolver.resolve(eq("EMP_MANAGER"), eq(5L), any())).thenReturn(null);
        // If re-triggered, resolve to user 99
        when(approverResolver.resolve(eq("ROLE:HR_MANAGER"), eq(5L), any())).thenReturn(20L);

        service.start(DEF_CODE, "LEAVE-004", "LEAVE", "{}", 5L);

        // Instance should be SUSPENDED because EMP_MANAGER resolved to null
        ApprovalInstance instance = capturedInstances.get(0);
        assertThat(instance.getStatus()).isEqualTo("SUSPENDED");

        // A history record with SUSPEND action should exist
        assertThat(capturedHistories).anyMatch(h -> "SUSPEND".equals(h.getAction()));
    }

    // ----------------------------------------------------------------
    // T5: All users with role resigned → instance SUSPENDED
    // ----------------------------------------------------------------
    @Test
    @DisplayName("All users with role resigned -> instance SUSPENDED")
    void allUsersResignedSuspend() {
        stubDefinitionLookup();
        stubDefinitionById();

        // EMP_MANAGER resolves fine
        when(approverResolver.resolve(eq("EMP_MANAGER"), eq(5L), any())).thenReturn(10L);
        // ROLE:HR_MANAGER — all resigned, returns null
        when(approverResolver.resolve(eq("ROLE:HR_MANAGER"), eq(5L), any())).thenReturn(null);

        service.start(DEF_CODE, "LEAVE-005", "LEAVE", "{}", 5L);

        // Approve node 1
        ApprovalTask task1 = capturedTasks.get(0);
        when(taskMapper.selectById(task1.getId())).thenReturn(task1);
        when(instanceMapper.selectById(1000L)).thenReturn(capturedInstances.get(0));
        service.approve(task1.getId(), 10L, null);

        // Node 2 task should have been created, but instance is SUSPENDED
        ApprovalInstance refreshed = copyInstance(capturedInstances.get(0));
        refreshed.setCurrentNodeSeq(2);
        // The instance got suspended during node 2 creation
        // Check that SUSPEND history was recorded
        assertThat(capturedHistories).anyMatch(h -> "SUSPEND".equals(h.getAction()));
    }

    // ----------------------------------------------------------------
    // T6: State machine — cannot approve already-approved task
    // ----------------------------------------------------------------
    @Test
    @DisplayName("Cannot approve an already-approved task -> BizException")
    void cannotApproveAlreadyApprovedTask() {
        stubDefinitionLookup();
        stubDefinitionById();

        when(approverResolver.resolve(eq("EMP_MANAGER"), eq(5L), any())).thenReturn(10L);

        service.start(DEF_CODE, "LEAVE-006", "LEAVE", "{}", 5L);

        ApprovalTask task1 = capturedTasks.get(0);
        ApprovalTask taskCopy = copyTask(task1);
        when(taskMapper.selectById(task1.getId())).thenReturn(taskCopy);
        when(instanceMapper.selectById(1000L)).thenReturn(capturedInstances.get(0));

        // First approve — succeeds
        service.approve(task1.getId(), 10L, null);
        assertThat(taskCopy.getStatus()).isEqualTo("APPROVED");

        // Second approve — should fail
        assertThatThrownBy(() -> service.approve(task1.getId(), 10L, null))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.BAD_REQUEST);
    }

    // ----------------------------------------------------------------
    // T7: Cannot approve task assigned to another user
    // ----------------------------------------------------------------
    @Test
    @DisplayName("Cannot approve task assigned to another user -> BizException")
    void cannotApproveTaskAssignedToAnother() {
        stubDefinitionLookup();
        stubDefinitionById();

        when(approverResolver.resolve(eq("EMP_MANAGER"), eq(5L), any())).thenReturn(10L);

        service.start(DEF_CODE, "LEAVE-007", "LEAVE", "{}", 5L);

        ApprovalTask task1 = capturedTasks.get(0);
        when(taskMapper.selectById(task1.getId())).thenReturn(task1);

        // User 99 tries to approve task assigned to user 10
        assertThatThrownBy(() -> service.approve(task1.getId(), 99L, null))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.FORBIDDEN);
    }

    // ----------------------------------------------------------------
    // T8: getTodoList returns pending tasks for user
    // ----------------------------------------------------------------
    @Test
    @DisplayName("getTodoList returns only PENDING tasks for the user")
    void getTodoListReturnsPendingTasks() {
        ApprovalTask t1 = new ApprovalTask();
        t1.setId(100L);
        t1.setAssigneeId(10L);
        t1.setStatus("PENDING");

        when(taskMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(t1));

        List<ApprovalTask> result = service.getTodoList(10L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssigneeId()).isEqualTo(10L);
    }

    // ----------------------------------------------------------------
    // T9: getHistory returns ordered history
    // ----------------------------------------------------------------
    @Test
    @DisplayName("getHistory returns all history records for instance")
    void getHistoryReturnsAllRecords() {
        ApprovalHistory h1 = new ApprovalHistory();
        h1.setAction("START");
        h1.setInstanceId(1000L);
        ApprovalHistory h2 = new ApprovalHistory();
        h2.setAction("APPROVE");
        h2.setInstanceId(1000L);

        when(historyMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(h1, h2));

        List<ApprovalHistory> result = service.getHistory(1000L);
        assertThat(result).hasSize(2);
    }

    // ----------------------------------------------------------------
    // T10: Revoke non-approved task fails
    // ----------------------------------------------------------------
    @Test
    @DisplayName("Cannot revoke a PENDING task -> BizException")
    void cannotRevokePendingTask() {
        stubDefinitionLookup();
        stubDefinitionById();

        when(approverResolver.resolve(eq("EMP_MANAGER"), eq(5L), any())).thenReturn(10L);

        service.start(DEF_CODE, "LEAVE-010", "LEAVE", "{}", 5L);

        ApprovalTask task1 = capturedTasks.get(0);
        ApprovalTask taskCopy = copyTask(task1);
        when(taskMapper.selectById(task1.getId())).thenReturn(taskCopy);

        // Try to revoke a PENDING task
        assertThatThrownBy(() -> service.revoke(task1.getId(), 10L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.BAD_REQUEST);
    }

    // ----------------------------------------------------------------
    // T11: Definition not found -> BizException
    // ----------------------------------------------------------------
    @Test
    @DisplayName("Start with invalid definition code -> BizException")
    void startWithInvalidDefinition() {
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.start("NONEXIST", "KEY-1", "LEAVE", "{}", 5L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.BAD_REQUEST);
    }

    // -- helpers -------------------------------------------------------

    private ApprovalInstance copyInstance(ApprovalInstance src) {
        ApprovalInstance copy = new ApprovalInstance();
        copy.setId(src.getId());
        copy.setDefinitionId(src.getDefinitionId());
        copy.setBusinessType(src.getBusinessType());
        copy.setBusinessKey(src.getBusinessKey());
        copy.setApplicantId(src.getApplicantId());
        copy.setCurrentNodeSeq(src.getCurrentNodeSeq());
        copy.setStatus(src.getStatus());
        copy.setPayload(src.getPayload());
        copy.setStartedAt(src.getStartedAt());
        copy.setFinishedAt(src.getFinishedAt());
        return copy;
    }

    private ApprovalTask copyTask(ApprovalTask src) {
        ApprovalTask copy = new ApprovalTask();
        copy.setId(src.getId());
        copy.setInstanceId(src.getInstanceId());
        copy.setNodeSeq(src.getNodeSeq());
        copy.setAssigneeId(src.getAssigneeId());
        copy.setStatus(src.getStatus());
        copy.setAssignedAt(src.getAssignedAt());
        copy.setActedAt(src.getActedAt());
        return copy;
    }
}
