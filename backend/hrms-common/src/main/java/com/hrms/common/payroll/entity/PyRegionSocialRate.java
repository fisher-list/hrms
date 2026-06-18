package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 多地区社保公积金比例配置。
 * <p>不同城市有不同的社保基数上下限和缴纳比例，员工按参保城市匹配。</p>
 * <p>不继承BaseEntity，因为这是静态参考表，无需审计字段。</p>
 */
@Data
@TableName("py_region_social_rate")
public class PyRegionSocialRate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 参保城市，如 "北京"、"上海"、"深圳" */
    private String city;

    /** 养老保险个人比例 */
    private BigDecimal pensionPersonal;

    /** 养老保险企业比例 */
    private BigDecimal pensionCompany;

    /** 医疗保险个人比例 */
    private BigDecimal medicalPersonal;

    /** 医疗保险固定费用 */
    private BigDecimal medicalFixedFee;

    /** 医疗保险企业比例 */
    private BigDecimal medicalCompany;

    /** 失业保险个人比例 */
    private BigDecimal unemploymentPersonal;

    /** 失业保险企业比例 */
    private BigDecimal unemploymentCompany;

    /** 工伤保险企业比例（个人不缴） */
    private BigDecimal injuryCompany;

    /** 生育保险企业比例（个人不缴） */
    private BigDecimal maternityCompany;

    /** 住房公积金个人比例 */
    private BigDecimal housingFundPersonal;

    /** 住房公积金企业比例 */
    private BigDecimal housingFundCompany;

    /** 社保基数下限 */
    private BigDecimal socialBaseFloor;

    /** 社保基数上限 */
    private BigDecimal socialBaseCeil;

    /** 公积金基数下限 */
    private BigDecimal fundBaseFloor;

    /** 公积金基数上限 */
    private BigDecimal fundBaseCeil;

    /** 是否启用 */
    private Boolean enabled;
}
