package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 年假配额规则实体。
 * <p>按司龄/职级自动生成年假天数，支持梯度规则配置。</p>
 * <p>规则示例：司龄1-10年=5天，10-20年=10天，20年以上=15天。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_leave_quota_rule")
public class AtLeaveQuotaRule extends BaseEntity {

    /** 规则名称，如"标准年假规则" */
    private String name;

    /** 规则编码，唯一标识 */
    private String code;

    /** 假期类型ID（关联at_leave_type），通常为年假 */
    private Long leaveTypeId;

    /** 司龄下限（含），单位：年 */
    private Integer seniorityMin;

    /** 司龄上限（不含），单位：年，null表示无上限 */
    private Integer seniorityMax;

    /** 职级下限（含），null表示不限 */
    private Integer gradeMin;

    /** 职级上限（不含），null表示不限 */
    private Integer gradeMax;

    /** 配额天数 */
    private BigDecimal quotaDays;

    /** 是否启用 */
    private Boolean enabled;

    /** 排序号（同司龄段内多规则时，取排序最小的匹配） */
    private Integer sortNo;
}
