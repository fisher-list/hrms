package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 试用期转正申请单实体。
 * 记录试用期员工的转正申请、考核结果及审批状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_probation_conversion")
public class HrProbationConversion extends BaseEntity {

    /** 关联员工ID */
    private Long employeeId;

    /** 转正申请单号 */
    private String conversionNo;

    /** 试用期开始日期 */
    private LocalDate probationStartDate;

    /** 试用期结束日期 */
    private LocalDate probationEndDate;

    /** 计划转正日期 */
    private LocalDate plannedConversionDate;

    /** 转正评估说明/自评 */
    private String evaluationRemark;

    /** 主管评语 */
    private String managerComment;

    /** 评估得分（可选，0-100） */
    private Integer evaluationScore;

    /** 状态: PENDING(待提交) / SUBMITTED(已提交) / APPROVED(已通过) / REJECTED(已驳回) */
    private String status;

    /** 关联审批实例ID */
    private Long approvalInstanceId;
}
