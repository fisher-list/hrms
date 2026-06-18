package com.hrms.common.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 银行代发批次创建DTO。
 */
@Data
public class BankPaymentCreateDto {

    /** 关联的薪酬运行ID（必须是LOCKED状态） */
    @NotNull
    private Long runId;

    /**
     * 银行类型：
     * GENERAL_CSV - 通用CSV格式
     * ICBC - 工商银行格式
     * CCB - 建设银行格式
     */
    @NotBlank
    private String bankType;

    /** 付款账户名称（公司名称） */
    @NotBlank
    private String payerAccountName;

    /** 付款账号 */
    @NotBlank
    private String payerAccountNo;

    /** 付款银行名称 */
    private String payerBankName;

    /** 备注 */
    private String remark;
}
