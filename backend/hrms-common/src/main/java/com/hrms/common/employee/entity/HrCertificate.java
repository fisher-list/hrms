package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工证明开具申请（在职证明/收入证明）。
 * <p>
 * type: EMPLOYMENT / INCOME
 * status: PENDING / APPROVED / REJECTED / ISSUED
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_certificate")
public class HrCertificate extends BaseEntity {

    /** 申请员工ID */
    private Long employeeId;

    /** 证明类型：EMPLOYMENT(在职证明) / INCOME(收入证明) */
    private String type;

    /** 用途说明 */
    private String purpose;

    /** 需要份数 */
    private Integer copies;

    /** 关联审批实例ID */
    private Long approvalInstanceId;

    /** PENDING / APPROVED / REJECTED / ISSUED */
    private String status;

    /** 签发日期 */
    private LocalDateTime issuedAt;

    /** 收入证明专用：起始日期 */
    private LocalDate incomeStartDate;

    /** 收入证明专用：截止日期 */
    private LocalDate incomeEndDate;

    /** 拒绝原因 */
    private String rejectReason;
}
