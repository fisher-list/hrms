package com.hrms.common.employee.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量导入结果VO。
 * 包含成功/失败统计及每行的详细错误信息。
 */
@Data
public class BatchImportResultVo {

    /** 总行数 */
    private int totalRows;

    /** 成功导入行数 */
    private int successCount;

    /** 失败行数 */
    private int failCount;

    /** 是否全部成功 */
    private boolean allSuccess;

    /** 每行的错误信息列表（行号 -> 错误描述） */
    private List<RowError> errors;

    /**
     * 单行错误信息。
     */
    @Data
    public static class RowError {
        /** Excel行号（从2开始，第1行为表头） */
        private int rowNum;
        /** 错误描述 */
        private String message;

        public RowError(int rowNum, String message) {
            this.rowNum = rowNum;
            this.message = message;
        }
    }
}
