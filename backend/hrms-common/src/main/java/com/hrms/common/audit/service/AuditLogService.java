package com.hrms.common.audit.service;

import com.hrms.common.audit.entity.SysAuditLog;
import com.hrms.common.audit.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service that persists audit logs asynchronously.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysAuditLogMapper auditLogMapper;

    @Async
    public void saveAsync(SysAuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage(), e);
        }
    }
}
