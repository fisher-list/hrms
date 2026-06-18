package com.hrms.common.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 合同续签申请创建DTO。
 */
@Data
public class ContractRenewalCreateDto {

    /** 员工ID */
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    /** 原合同ID */
    @NotNull(message = "原合同ID不能为空")
    private Long originalContractId;

    /** 新合同类型: FIXED / PERMANENT */
    @NotNull(message = "新合同类型不能为空")
    private String newContractType;

    /** 新合同开始日期 */
    @NotNull(message = "新合同开始日期不能为空")
    private LocalDate newStartDate;

    /** 新合同结束日期（无固定期限合同可为空） */
    private LocalDate newEndDate;

    /** 续签原因/备注 */
    private String remark;
}
