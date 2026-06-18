package com.hrms.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.hrms.common.security.LoginUser;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Populates BaseEntity housekeeping fields on insert/update so business code does not
 * have to remember them.  {@code created_by} / {@code updated_by} are filled from
 * the current {@link LoginUser} in the security context.  When no authenticated user
 * is present (e.g. system init, scheduled tasks), these fields are left {@code null}.
 */
@Component
public class AuditFieldFiller implements MetaObjectHandler {

    private final Clock clock;

    public AuditFieldFiller(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(clock);
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "tenantId", Long.class, 1L);
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            strictInsertFill(metaObject, "createdBy", Long.class, currentUserId);
            strictInsertFill(metaObject, "updatedBy", Long.class, currentUserId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now(clock));
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId);
        }
    }

    /**
     * Returns the current authenticated user's uid, or {@code null} when running
     * outside a security context (e.g. system init, scheduled tasks).
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUid();
        }
        return null;
    }
}
