package com.hrms.common.attendance.service;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.dto.TimePunchCreateDto;
import com.hrms.common.attendance.entity.AtTimePunch;
import com.hrms.common.attendance.mapper.AtTimePunchMapper;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service for time punch management and CSV import.
 */
@Service
@RequiredArgsConstructor
public class TimePunchService {

    private final AtTimePunchMapper punchMapper;
    private final HrEmployeeMapper employeeMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_IMPORT_ROWS = 5_000;

    /**
     * Manual single record creation.
     */
    @Transactional
    public AtTimePunch create(TimePunchCreateDto dto) {
        AtTimePunch punch = new AtTimePunch();
        punch.setEmployeeId(dto.getEmployeeId());
        punch.setPunchDate(dto.getPunchDate());
        punch.setClockIn(dto.getClockIn());
        punch.setClockOut(dto.getClockOut());
        punch.setSource("MANUAL");
        punchMapper.insert(punch);
        return punch;
    }

    /**
     * Import CSV: emp_no,date,clock_in,clock_out
     * Returns success count and error list.
     */
    @Transactional
    public Map<String, Object> importCsv(InputStream csvInputStream) {
        List<Map<String, Object>> errors = new ArrayList<>();
        List<AtTimePunch> toInsert = new ArrayList<>();

        List<CsvRow> rows = readRows(csvInputStream);
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new BizException(BizCode.BAD_REQUEST, "CSV 行数不能超过 " + MAX_IMPORT_ROWS + " 行");
        }
        int rowNum = 1; // header is row 0

        for (CsvRow row : rows) {
            rowNum++;
            if (row.getFieldCount() < 4) {
                errors.add(Map.of("line", rowNum, "reason", "Insufficient columns"));
                continue;
            }
            String empNo = row.get(0).trim();
            String dateStr = row.get(1).trim();
            String clockIn = row.get(2).trim();
            String clockOut = row.get(3).trim();

            // Validate emp_no exists
            HrEmployee emp = employeeMapper.selectOne(
                    new LambdaQueryWrapper<HrEmployee>().eq(HrEmployee::getEmpNo, empNo));
            if (emp == null) {
                errors.add(Map.of("line", rowNum, "reason", "Employee not found: " + empNo));
                continue;
            }

            // Validate date format
            LocalDate punchDate;
            try {
                punchDate = LocalDate.parse(dateStr, DATE_FMT);
            } catch (DateTimeParseException e) {
                errors.add(Map.of("line", rowNum, "reason", "Invalid date format: " + dateStr));
                continue;
            }

            // Validate clock_in < clock_out
            if (clockIn.compareTo(clockOut) >= 0) {
                errors.add(Map.of("line", rowNum, "reason", "clock_in must be before clock_out: " + clockIn + " >= " + clockOut));
                continue;
            }

            AtTimePunch punch = new AtTimePunch();
            punch.setEmployeeId(emp.getId());
            punch.setPunchDate(punchDate);
            punch.setClockIn(clockIn);
            punch.setClockOut(clockOut);
            punch.setSource("CSV");
            toInsert.add(punch);
        }

        // If any errors, do not insert any rows
        if (!errors.isEmpty()) {
            return Map.of("successCount", 0, "errors", errors);
        }

        // Batch insert
        for (AtTimePunch punch : toInsert) {
            punchMapper.insert(punch);
        }

        return Map.of("successCount", toInsert.size(), "errors", Collections.emptyList());
    }

    private List<CsvRow> readRows(InputStream csvInputStream) {
        if (csvInputStream == null) {
            throw new BizException(BizCode.BAD_REQUEST, "CSV 文件不能为空");
        }
        try {
            CsvReadConfig config = new CsvReadConfig();
            config.setContainsHeader(true);
            CsvReader reader = new CsvReader(new InputStreamReader(csvInputStream, StandardCharsets.UTF_8), config);
            CsvData data = reader.read();
            return data.getRows();
        } catch (Exception e) {
            throw new BizException(BizCode.BAD_REQUEST, "CSV 文件读取失败，请检查文件编码和格式");
        }
    }

    /**
     * List punch records with optional filters.
     */
    public List<AtTimePunch> list(Long employeeId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AtTimePunch> qw = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            qw.eq(AtTimePunch::getEmployeeId, employeeId);
        }
        if (startDate != null) {
            qw.ge(AtTimePunch::getPunchDate, startDate);
        }
        if (endDate != null) {
            qw.le(AtTimePunch::getPunchDate, endDate);
        }
        qw.orderByAsc(AtTimePunch::getPunchDate);
        return punchMapper.selectList(qw);
    }
}
