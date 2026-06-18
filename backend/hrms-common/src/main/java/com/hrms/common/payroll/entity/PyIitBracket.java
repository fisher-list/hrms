package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * IIT progressive tax bracket.
 * No BaseEntity because this is a static reference table without audit fields.
 */
@Data
@TableName("py_iit_bracket")
public class PyIitBracket implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private BigDecimal lowerLimit;

    private BigDecimal upperLimit;

    private BigDecimal rate;

    private BigDecimal quickDeduction;
}
