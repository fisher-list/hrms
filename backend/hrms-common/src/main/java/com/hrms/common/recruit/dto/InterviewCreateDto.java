package com.hrms.common.recruit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for scheduling an interview.
 */
@Data
public class InterviewCreateDto {

    @NotNull
    private Long candidateId;

    @NotNull
    private Integer roundNo;

    private Long interviewerUserId;

    private LocalDateTime interviewTime;

    private String location;
}
