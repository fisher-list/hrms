package com.hrms.common.recruit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 职位发布记录实体。
 * <p>记录职位需求在各渠道的发布情况，支持一键发布到多个渠道。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rc_requisition_publish")
public class RcRequisitionPublish extends BaseEntity {

    /** 职位需求ID（关联rc_job_requisition） */
    private Long requisitionId;

    /** 发布渠道：OFFICIAL（官网）、ZHILIAN（智联招聘）、BOSS（BOSS直聘）、LAGOUP（拉勾）、51JOB */
    private String channel;

    /** 发布渠道的URL或链接 */
    private String publishUrl;

    /** 发布状态：PENDING / PUBLISHED / FAILED / CLOSED */
    private String status;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 关闭时间 */
    private LocalDateTime closedAt;

    /** 失败原因（状态为FAILED时填写） */
    private String failReason;

    /** 发布浏览量（由外部回调更新） */
    private Integer viewCount;

    /** 投递量（由外部回调更新） */
    private Integer applyCount;
}
