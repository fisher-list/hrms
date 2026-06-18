package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Transfer form: captures department/position transfer for approval workflow.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_transfer_form")
public class HrTransferForm extends BaseEntity {

    private String formNo;

    private Long employeeId;

    private Long fromDeptId;

    private Long fromPositionId;

    private Long toDeptId;

    private Long toPositionId;

    /** DEPARTMENT / POSITION / BOTH */
    private String transferType;

    private LocalDate effectiveDate;

    private String reason;

    /** PENDING / SUBMITTED / APPROVED / REJECTED */
    private String status;

    private Long approvalInstanceId;
}
