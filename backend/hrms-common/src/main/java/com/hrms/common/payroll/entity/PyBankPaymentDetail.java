package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 银行代发明细实体类。
 * 每行代表一个员工的代发记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_bank_payment_detail")
public class PyBankPaymentDetail extends BaseEntity {

    /** 关联的银行代发批次ID */
    private Long batchId;

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名 */
    private String employeeName;

    /** 员工编号 */
    private String empNo;

    /** 收款银行名称 */
    private String bankName;

    /** 收款账号（明文，用于生成文件） */
    private String accountNo;

    /** 收款人姓名 */
    private String accountName;

    /** 代发金额（等于该员工的净工资） */
    private BigDecimal amount;

    /** 用途/摘要 */
    private String purpose;
}
