package com.hrms.common.recruit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 职位一键发布请求DTO。
 */
@Data
public class RequisitionPublishDto {

    /** 职位需求ID */
    @NotNull
    private Long requisitionId;

    /** 发布渠道列表，如 ["OFFICIAL", "ZHILIAN", "BOSS"] */
    @NotEmpty
    private List<String> channels;
}
