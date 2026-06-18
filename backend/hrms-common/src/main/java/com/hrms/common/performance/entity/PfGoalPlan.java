package com.hrms.common.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 绩效目标计划 —— 支持组织→部门→个人的层层分解对齐。
 * <p>
 * level: ORG / DEPT / PERSON
 * status: DRAFT / CONFIRMED / IN_PROGRESS / COMPLETED
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_goal_plan")
public class PfGoalPlan extends BaseEntity {

    /** 关联绩效周期ID */
    private Long cycleId;

    /** 父目标ID，顶级目标为null */
    private Long parentId;

    /** 目标层级：ORG / DEPT / PERSON */
    private String goalLevel;

    /** 所有者类型：ORG / DEPT / PERSON */
    private String ownerType;

    /** 所有者ID（组织ID / 部门ID / 员工ID） */
    private Long ownerId;

    /** 目标标题 */
    private String title;

    /** 目标描述 */
    private String description;

    /** 权重百分比 */
    private Integer weight;

    /** 目标值（数值型KPI） */
    private BigDecimal targetValue;

    /** 实际完成值 */
    private BigDecimal actualValue;

    /** DRAFT / CONFIRMED / IN_PROGRESS / COMPLETED */
    private String status;

    private Integer sortOrder;
}
