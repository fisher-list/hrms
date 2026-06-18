package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.dto.BatchImportResultVo;
import com.hrms.common.employee.dto.EmployeeExportVo;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.entity.Department;
import com.hrms.common.org.entity.Position;
import com.hrms.common.org.mapper.DepartmentMapper;
import com.hrms.common.org.mapper.PositionMapper;
import com.hrms.common.util.AesUtil;
import com.hrms.common.util.MaskUtil;
import com.hrms.common.util.SensitiveHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 员工批量导入导出服务。
 * 导入：从Excel解析员工信息，逐行校验，返回导入结果。
 * 导出：查询全部员工生成花名册Excel。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeBatchService {

    private final HrEmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;

    /** Excel表头定义（导入模板列顺序） */
    private static final String[] IMPORT_HEADERS = {
            "姓名", "性别", "出生日期", "身份证号", "手机号", "邮箱",
            "部门ID", "岗位ID", "入职日期", "合同开始日期", "合同结束日期",
            "试用期结束日期", "紧急联系人", "紧急联系人电话"
    };

    /** 日期格式 */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 从Excel文件批量导入员工信息。
     * 逐行解析并校验，成功行直接入库，失败行记录错误信息。
     *
     * @param file 上传的Excel文件
     * @return 导入结果（含成功/失败统计及行级错误信息）
     */
    @Transactional
    public BatchImportResultVo importFromExcel(MultipartFile file) {
        BatchImportResultVo result = new BatchImportResultVo();
        List<BatchImportResultVo.RowError> errors = new ArrayList<>();
        int totalRows = 0;
        int successCount = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            // 校验表头
            if (lastRow < 1) {
                throw new BizException(BizCode.BATCH_IMPORT_FORMAT_ERROR, "Excel文件为空或缺少数据行");
            }
            validateHeaders(sheet.getRow(0));

            // 从第2行开始遍历数据行
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }
                totalRows++;

                try {
                    // 解析并校验单行数据
                    HrEmployee emp = parseRow(row, i + 1);

                    // 身份证去重检查
                    String idCardHash = emp.getIdCardHash();
                    if (idCardHash != null) {
                        long dupCount = employeeMapper.selectCount(
                                new LambdaQueryWrapper<HrEmployee>()
                                        .eq(HrEmployee::getIdCardHash, idCardHash));
                        if (dupCount > 0) {
                            errors.add(new BatchImportResultVo.RowError(i + 1, "身份证号已存在"));
                            continue;
                        }
                    }

                    emp.setStatus("PENDING_HIRE");
                    employeeMapper.insert(emp);
                    emp.setEmpNo(String.format("E%06d", emp.getId()));
                    employeeMapper.updateById(emp);
                    successCount++;
                } catch (Exception e) {
                    errors.add(new BatchImportResultVo.RowError(i + 1, e.getMessage()));
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(BizCode.BATCH_IMPORT_FORMAT_ERROR, "Excel文件解析失败: " + e.getMessage());
        }

        result.setTotalRows(totalRows);
        result.setSuccessCount(successCount);
        result.setFailCount(totalRows - successCount);
        result.setAllSuccess(errors.isEmpty());
        result.setErrors(errors);
        return result;
    }

    /**
     * 导出员工花名册为Excel文件。
     *
     * @param status 员工状态筛选（可选）
     * @return Excel文件字节数组
     */
    public byte[] exportToExcel(String status) {
        // 查询员工列表
        LambdaQueryWrapper<HrEmployee> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(HrEmployee::getStatus, status);
        }
        qw.orderByAsc(HrEmployee::getEmpNo);
        List<HrEmployee> employees = employeeMapper.selectList(qw);

        // 预加载部门和岗位名称映射
        Map<Long, String> deptNameMap = buildDeptNameMap();
        Map<Long, String> posNameMap = buildPositionNameMap();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("员工花名册");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 写入表头
            String[] exportHeaders = {
                    "工号", "姓名", "性别", "出生日期", "身份证号(脱敏)", "手机号(脱敏)",
                    "邮箱", "部门", "岗位", "入职日期", "状态",
                    "合同开始", "合同结束", "试用期结束", "紧急联系人"
            };
            Row headerRow = sheet.createRow(0);
            for (int j = 0; j < exportHeaders.length; j++) {
                Cell cell = headerRow.createCell(j);
                cell.setCellValue(exportHeaders[j]);
                cell.setCellStyle(headerStyle);
            }

            // 写入数据行
            int rowNum = 1;
            for (HrEmployee emp : employees) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;
                row.createCell(col++).setCellValue(emp.getEmpNo() != null ? emp.getEmpNo() : "");
                row.createCell(col++).setCellValue(emp.getName() != null ? emp.getName() : "");
                row.createCell(col++).setCellValue(formatGender(emp.getGender()));
                row.createCell(col++).setCellValue(emp.getBirthDate() != null ? emp.getBirthDate().format(DATE_FMT) : "");
                // 身份证脱敏
                row.createCell(col++).setCellValue(maskIdCard(emp.getIdCardEnc()));
                // 手机脱敏
                row.createCell(col++).setCellValue(maskPhone(emp.getPhoneEnc()));
                row.createCell(col++).setCellValue(emp.getEmail() != null ? emp.getEmail() : "");
                row.createCell(col++).setCellValue(deptNameMap.getOrDefault(emp.getDeptId(), ""));
                row.createCell(col++).setCellValue(posNameMap.getOrDefault(emp.getPositionId(), ""));
                row.createCell(col++).setCellValue(emp.getHireDate() != null ? emp.getHireDate().format(DATE_FMT) : "");
                row.createCell(col++).setCellValue(formatStatus(emp.getStatus()));
                row.createCell(col++).setCellValue(emp.getContractStart() != null ? emp.getContractStart().format(DATE_FMT) : "");
                row.createCell(col++).setCellValue(emp.getContractEnd() != null ? emp.getContractEnd().format(DATE_FMT) : "");
                row.createCell(col++).setCellValue(emp.getProbationEnd() != null ? emp.getProbationEnd().format(DATE_FMT) : "");
                row.createCell(col++).setCellValue(emp.getEmergencyContact() != null ? emp.getEmergencyContact() : "");
            }

            // 自动调整列宽
            for (int j = 0; j < exportHeaders.length; j++) {
                sheet.autoSizeColumn(j);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(BizCode.INTERNAL_ERROR, "导出Excel失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验Excel表头是否匹配模板。
     */
    private void validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new BizException(BizCode.BATCH_IMPORT_FORMAT_ERROR, "缺少表头行");
        }
        for (int i = 0; i < IMPORT_HEADERS.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !IMPORT_HEADERS[i].equals(getCellStringValue(cell).trim())) {
                throw new BizException(BizCode.BATCH_IMPORT_FORMAT_ERROR,
                        "表头第" + (i + 1) + "列应为「" + IMPORT_HEADERS[i] + "」");
            }
        }
    }

    /**
     * 解析单行Excel数据为HrEmployee实体，含校验逻辑。
     */
    private HrEmployee parseRow(Row row, int rowNum) {
        HrEmployee emp = new HrEmployee();

        // 姓名（必填）
        String name = getCellStringValue(row.getCell(0)).trim();
        if (name.isEmpty()) {
            throw new BizException(BizCode.BATCH_IMPORT_DATA_ERROR, "第" + rowNum + "行: 姓名不能为空");
        }
        emp.setName(name);

        // 性别（必填，M/F）
        String gender = getCellStringValue(row.getCell(1)).trim();
        if (!"M".equalsIgnoreCase(gender) && !"F".equalsIgnoreCase(gender)
                && !"男".equals(gender) && !"女".equals(gender)) {
            throw new BizException(BizCode.BATCH_IMPORT_DATA_ERROR, "第" + rowNum + "行: 性别应为M/F/男/女");
        }
        emp.setGender("男".equals(gender) || "M".equalsIgnoreCase(gender) ? "M" : "F");

        // 出生日期
        String birthDateStr = getCellStringValue(row.getCell(2)).trim();
        if (!birthDateStr.isEmpty()) {
            emp.setBirthDate(parseDate(birthDateStr, rowNum, "出生日期"));
        }

        // 身份证号（加密存储，哈希用于去重）
        String idCard = getCellStringValue(row.getCell(3)).trim();
        if (!idCard.isEmpty()) {
            emp.setIdCardEnc(AesUtil.encrypt(idCard));
            emp.setIdCardHash(SensitiveHashUtil.idCardHash(idCard));
        }

        // 手机号（加密存储）
        String phone = getCellStringValue(row.getCell(4)).trim();
        if (!phone.isEmpty()) {
            emp.setPhoneEnc(AesUtil.encrypt(phone));
        }

        // 邮箱
        String email = getCellStringValue(row.getCell(5)).trim();
        if (!email.isEmpty()) {
            emp.setEmail(email);
        }

        // 部门ID（必填）
        String deptIdStr = getCellStringValue(row.getCell(6)).trim();
        if (deptIdStr.isEmpty()) {
            throw new BizException(BizCode.BATCH_IMPORT_DATA_ERROR, "第" + rowNum + "行: 部门ID不能为空");
        }
        try {
            emp.setDeptId(Long.parseLong(deptIdStr));
        } catch (NumberFormatException e) {
            throw new BizException(BizCode.BATCH_IMPORT_DATA_ERROR, "第" + rowNum + "行: 部门ID格式错误");
        }

        // 岗位ID（必填）
        String posIdStr = getCellStringValue(row.getCell(7)).trim();
        if (posIdStr.isEmpty()) {
            throw new BizException(BizCode.BATCH_IMPORT_DATA_ERROR, "第" + rowNum + "行: 岗位ID不能为空");
        }
        try {
            emp.setPositionId(Long.parseLong(posIdStr));
        } catch (NumberFormatException e) {
            throw new BizException(BizCode.BATCH_IMPORT_DATA_ERROR, "第" + rowNum + "行: 岗位ID格式错误");
        }

        // 入职日期（必填）
        String hireDateStr = getCellStringValue(row.getCell(8)).trim();
        if (hireDateStr.isEmpty()) {
            throw new BizException(BizCode.BATCH_IMPORT_DATA_ERROR, "第" + rowNum + "行: 入职日期不能为空");
        }
        emp.setHireDate(parseDate(hireDateStr, rowNum, "入职日期"));

        // 合同开始日期
        String contractStartStr = getCellStringValue(row.getCell(9)).trim();
        if (!contractStartStr.isEmpty()) {
            emp.setContractStart(parseDate(contractStartStr, rowNum, "合同开始日期"));
        }

        // 合同结束日期
        String contractEndStr = getCellStringValue(row.getCell(10)).trim();
        if (!contractEndStr.isEmpty()) {
            emp.setContractEnd(parseDate(contractEndStr, rowNum, "合同结束日期"));
        }

        // 试用期结束日期
        String probationEndStr = getCellStringValue(row.getCell(11)).trim();
        if (!probationEndStr.isEmpty()) {
            emp.setProbationEnd(parseDate(probationEndStr, rowNum, "试用期结束日期"));
        }

        // 紧急联系人
        String emergencyContact = getCellStringValue(row.getCell(12)).trim();
        if (!emergencyContact.isEmpty()) {
            emp.setEmergencyContact(emergencyContact);
        }

        // 紧急联系人电话（加密存储）
        String emergencyPhone = getCellStringValue(row.getCell(13)).trim();
        if (!emergencyPhone.isEmpty()) {
            emp.setEmergencyPhoneEnc(AesUtil.encrypt(emergencyPhone));
        }

        return emp;
    }

    /**
     * 解析日期字符串。
     */
    private LocalDate parseDate(String dateStr, int rowNum, String fieldName) {
        try {
            return LocalDate.parse(dateStr, DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new BizException(BizCode.BATCH_IMPORT_DATA_ERROR,
                    "第" + rowNum + "行: " + fieldName + "格式错误，应为yyyy-MM-dd");
        }
    }

    /**
     * 获取单元格字符串值（兼容数字和日期类型）。
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FMT);
                }
                // 数字去掉末尾.0
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                    return String.valueOf((long) numVal);
                }
                return String.valueOf(numVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * 判断是否为空行。
     */
    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < IMPORT_HEADERS.length; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !getCellStringValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 构建部门ID->名称映射。
     */
    private Map<Long, String> buildDeptNameMap() {
        List<Department> depts = departmentMapper.selectList(null);
        Map<Long, String> map = new HashMap<>();
        for (Department dept : depts) {
            map.put(dept.getId(), dept.getName());
        }
        return map;
    }

    /**
     * 构建岗位ID->名称映射。
     */
    private Map<Long, String> buildPositionNameMap() {
        List<Position> positions = positionMapper.selectList(null);
        Map<Long, String> map = new HashMap<>();
        for (Position pos : positions) {
            map.put(pos.getId(), pos.getName());
        }
        return map;
    }

    /**
     * 格式化性别显示。
     */
    private String formatGender(String gender) {
        if ("M".equals(gender)) return "男";
        if ("F".equals(gender)) return "女";
        return gender != null ? gender : "";
    }

    /**
     * 格式化状态显示。
     */
    private String formatStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "PENDING_HIRE": return "待入职";
            case "PROBATION": return "试用期";
            case "ACTIVE": return "在职";
            case "ON_LEAVE": return "休假中";
            case "TERMINATED": return "已离职";
            default: return status;
        }
    }

    /**
     * 脱敏身份证号。
     */
    private String maskIdCard(String idCardEnc) {
        if (idCardEnc == null || idCardEnc.isEmpty()) return "";
        try {
            return MaskUtil.maskIdCard(AesUtil.decrypt(idCardEnc));
        } catch (Exception e) {
            return "***";
        }
    }

    /**
     * 脱敏手机号。
     */
    private String maskPhone(String phoneEnc) {
        if (phoneEnc == null || phoneEnc.isEmpty()) return "";
        try {
            return MaskUtil.maskPhone(AesUtil.decrypt(phoneEnc));
        } catch (Exception e) {
            return "***";
        }
    }
}
