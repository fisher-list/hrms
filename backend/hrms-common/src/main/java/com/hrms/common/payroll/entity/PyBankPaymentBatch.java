package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 银行代发批次实体类。
 * 根据锁定的薪酬运行批次生成银行代发文件，支持多种银行格式。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_bank_payment_batch")
public class PyBankPaymentBatch extends BaseEntity {

    /** 关联的薪酬运行ID（必须是LOCKED状态） */
    private Long runId;

    /** 批次号，自动生成：BP + 年月日 + 序号 */
    private String batchNo;

    /** 银行类型：GENERAL_CSV / ICBC / CCB */
    private String bankType;

    /** 付款账户名称（公司名称） */
    private String payerAccountName;

    /** 付款账号 */
    private String payerAccountNo;

    /** 付款银行名称 */
    private String payerBankName;

    /** 员工人数 */
    private Integer employeeCount;

    /** 代发总金额 */
    private BigDecimal totalAmount;

    /** 状态：GENERATED / EXPORTED / CONFIRMED */
    private String status;

    /** 生成的文件路径/文件名 */
    private String filePath;

    /** 备注 */
    private String remark;
}
