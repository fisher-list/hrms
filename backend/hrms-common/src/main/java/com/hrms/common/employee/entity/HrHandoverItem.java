package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 离职工作交接项：记录资产归还、文档交接、项目交接等。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_handover_item")
public class HrHandoverItem extends BaseEntity {

    /** 关联的离职申请单ID */
    private Long formId;

    /** 交接类别：ASSET / DOCUMENT / PROJECT / ACCOUNT / OTHER */
    private String category;

    /** 交接内容描述 */
    private String description;

    /** 交接接收人（姓名或工号） */
    private String handoverTo;

    /** 状态：PENDING / COMPLETED */
    private String status;

    /** 备注 */
    private String remark;
}
