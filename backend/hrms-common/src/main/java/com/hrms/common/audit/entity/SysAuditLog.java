package com.hrms.common.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Audit log entity recording every audited operation.
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String username;

    private String action;

    private String resourceType;

    private String resourceId;

    private String ipAddress;

    private String userAgent;

    private String requestMethod;

    private String requestUri;

    private Integer statusCode;

    private LocalDateTime createdAt;
}
