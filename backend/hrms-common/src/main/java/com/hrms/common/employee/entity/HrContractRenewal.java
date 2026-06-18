package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 合同续签申请单实体。
 * 记录合同续签的申请信息、新合同期限及审批状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_contract_renewal")
public class HrContractRenewal extends BaseEntity {

    /** 关联员工ID */
    private Long employeeId;

    /** 原合同ID（关联hr_employee_contract.id） */
    private Long originalContractId;

    /** 续签申请单号 */
    private String renewalNo;

    /** 新合同类型: FIXED(固定期限) / PERMANENT(无固定期限) */
    private String newContractType;

    /** 新合同开始日期 */
    private LocalDate newStartDate;

    /** 新合同结束日期（无固定期限合同可为空） */
    private LocalDate newEndDate;

    /** 续签原因/备注 */
    private String remark;

    /** 状态: PENDING(待提交) / SUBMITTED(已提交) / APPROVED(已通过) / REJECTED(已驳回) */
    private String status;

    /** 关联审批实例ID */
    private Long approvalInstanceId;
}
