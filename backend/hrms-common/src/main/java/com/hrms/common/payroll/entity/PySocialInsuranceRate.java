package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Social insurance and housing fund rate configuration.
 * No BaseEntity because this is a static reference table.
 */
@Data
@TableName("py_social_insurance_rate")
public class PySocialInsuranceRate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private BigDecimal pensionPersonal;

    private BigDecimal pensionCompany;

    private BigDecimal medicalPersonal;

    private BigDecimal medicalFixedFee;

    private BigDecimal medicalCompany;

    private BigDecimal unemploymentPersonal;

    private BigDecimal unemploymentCompany;

    private BigDecimal housingFundPersonal;

    private BigDecimal housingFundCompany;
}
