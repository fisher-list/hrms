package com.hrms.common.approval.dto;

import lombok.Data;

/**
 * DTO for approve/reject actions on a task.
 */
@Data
public class CommentDto {

    /** Optional comment for the approval action */
    private String comment;
}
