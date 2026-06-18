package com.hrms.common.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.org.entity.Department;
import com.hrms.common.org.mapper.DepartmentMapper;
import com.hrms.common.payroll.entity.PyPayrollDetail;
import com.hrms.common.payroll.entity.PyPayrollPeriod;
import com.hrms.common.payroll.entity.PyPayrollRun;
import com.hrms.common.payroll.entity.PySocialInsuranceRate;
import com.hrms.common.payroll.mapper.PyPayrollDetailMapper;
import com.hrms.common.payroll.mapper.PyPayrollPeriodMapper;
import com.hrms.common.payroll.mapper.PyPayrollRunMapper;
import com.hrms.common.payroll.mapper.PySocialInsuranceRateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 法定报表导出服务 —— 社保/个税/公积金申报表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatutoryReportService {

    private final PyPayrollPeriodMapper periodMapper;
    private final PyPayrollRunMapper runMapper;
    private final PyPayrollDetailMapper detailMapper;
    private final HrEmployeeMapper employeeMapper;
    private final DepartmentMapper deptMapper;
    private final PySocialInsuranceRateMapper siRateMapper;

    /**
     * 导出社保申报表。
     */
    public byte[] exportSocialInsuranceReport(String periodMonth) throws IOException {
        List<PyPayrollDetail> details = getDetailsForPeriod(periodMonth);
        PySocialInsuranceRate rate = getLatestSIRate();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("社保申报表");

            // 标题行
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("社会保险申报表 - " + periodMonth);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

            // 表头
            String[] headers = {"序号", "姓名", "工号", "部门", "养老保险(个人)", "养老保险(单位)",
                    "医疗保险(个人)", "医疗保险(单位)", "失业保险(个人)", "失业保险(单位)", "个人合计", "单位合计"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 数据行
            int rowIdx = 2;
            int seq = 1;
            for (PyPayrollDetail detail : details) {
                HrEmployee emp = employeeMapper.selectById(detail.getEmployeeId());
                if (emp == null) continue;

                BigDecimal grossPay = detail.getGrossPay();
                BigDecimal pensionP = grossPay.multiply(rate.getPensionPersonal()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal pensionC = grossPay.multiply(rate.getPensionCompany()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal medicalP = grossPay.multiply(rate.getMedicalPersonal()).add(rate.getMedicalFixedFee()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal medicalC = grossPay.multiply(rate.getMedicalCompany()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal unemployP = grossPay.multiply(rate.getUnemploymentPersonal()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal unemployC = grossPay.multiply(rate.getUnemploymentCompany()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal personalTotal = pensionP.add(medicalP).add(unemployP);
                BigDecimal companyTotal = pensionC.add(medicalC).add(unemployC);

                Department dept = emp.getDeptId() != null ? deptMapper.selectById(emp.getDeptId()) : null;
                String deptName = dept != null ? dept.getName() : "";

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(seq++);
                row.createCell(1).setCellValue(emp.getName());
                row.createCell(2).setCellValue(emp.getEmpNo());
                row.createCell(3).setCellValue(deptName);
                row.createCell(4).setCellValue(pensionP.doubleValue());
                row.createCell(5).setCellValue(pensionC.doubleValue());
                row.createCell(6).setCellValue(medicalP.doubleValue());
                row.createCell(7).setCellValue(medicalC.doubleValue());
                row.createCell(8).setCellValue(unemployP.doubleValue());
                row.createCell(9).setCellValue(unemployC.doubleValue());
                row.createCell(10).setCellValue(personalTotal.doubleValue());
                row.createCell(11).setCellValue(companyTotal.doubleValue());
            }

            return toBytes(wb);
        }
    }

    /**
     * 导出个税申报表。
     */
    public byte[] exportIitReport(String periodMonth) throws IOException {
        List<PyPayrollDetail> details = getDetailsForPeriod(periodMonth);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("个税申报表");

            // 标题行
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("个人所得税申报表 - " + periodMonth);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // 表头
            String[] headers = {"序号", "姓名", "工号", "身份证号", "应发工资", "社保扣除", "应纳税所得额", "个税金额"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 数据行
            int rowIdx = 2;
            int seq = 1;
            for (PyPayrollDetail detail : details) {
                HrEmployee emp = employeeMapper.selectById(detail.getEmployeeId());
                if (emp == null) continue;

                BigDecimal taxableIncome = detail.getGrossPay()
                        .subtract(detail.getSocialInsurance())
                        .subtract(detail.getHousingFund())
                        .subtract(new BigDecimal("5000")); // 起征点

                if (taxableIncome.compareTo(BigDecimal.ZERO) < 0) {
                    taxableIncome = BigDecimal.ZERO;
                }

                Department dept = emp.getDeptId() != null ? deptMapper.selectById(emp.getDeptId()) : null;

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(seq++);
                row.createCell(1).setCellValue(emp.getName());
                row.createCell(2).setCellValue(emp.getEmpNo());
                row.createCell(3).setCellValue(""); // 身份证号脱敏
                row.createCell(4).setCellValue(detail.getGrossPay().doubleValue());
                row.createCell(5).setCellValue(detail.getSocialInsurance().add(detail.getHousingFund()).doubleValue());
                row.createCell(6).setCellValue(taxableIncome.doubleValue());
                row.createCell(7).setCellValue(detail.getIit().doubleValue());
            }

            return toBytes(wb);
        }
    }

    /**
     * 导出公积金申报表。
     */
    public byte[] exportHousingFundReport(String periodMonth) throws IOException {
        List<PyPayrollDetail> details = getDetailsForPeriod(periodMonth);
        PySocialInsuranceRate rate = getLatestSIRate();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("公积金申报表");

            // 标题行
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("住房公积金申报表 - " + periodMonth);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // 表头
            String[] headers = {"序号", "姓名", "工号", "部门", "缴存基数", "个人缴存", "单位缴存"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 数据行
            int rowIdx = 2;
            int seq = 1;
            for (PyPayrollDetail detail : details) {
                HrEmployee emp = employeeMapper.selectById(detail.getEmployeeId());
                if (emp == null) continue;

                BigDecimal base = detail.getGrossPay();
                BigDecimal hfPersonal = base.multiply(rate.getHousingFundPersonal()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal hfCompany = base.multiply(rate.getHousingFundCompany()).setScale(2, RoundingMode.HALF_UP);

                Department dept = emp.getDeptId() != null ? deptMapper.selectById(emp.getDeptId()) : null;
                String deptName = dept != null ? dept.getName() : "";

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(seq++);
                row.createCell(1).setCellValue(emp.getName());
                row.createCell(2).setCellValue(emp.getEmpNo());
                row.createCell(3).setCellValue(deptName);
                row.createCell(4).setCellValue(base.doubleValue());
                row.createCell(5).setCellValue(hfPersonal.doubleValue());
                row.createCell(6).setCellValue(hfCompany.doubleValue());
            }

            return toBytes(wb);
        }
    }

    /**
     * 获取指定期间的薪资明细。
     */
    private List<PyPayrollDetail> getDetailsForPeriod(String periodMonth) {
        PyPayrollPeriod period = periodMapper.selectOne(
                new LambdaQueryWrapper<PyPayrollPeriod>()
                        .eq(PyPayrollPeriod::getPeriodMonth, periodMonth));
        if (period == null) {
            return List.of();
        }

        PyPayrollRun run = runMapper.selectOne(
                new LambdaQueryWrapper<PyPayrollRun>()
                        .eq(PyPayrollRun::getPeriodId, period.getId())
                        .eq(PyPayrollRun::getRunType, "NORMAL")
                        .orderByDesc(PyPayrollRun::getCreatedAt)
                        .last("LIMIT 1"));
        if (run == null) {
            return List.of();
        }

        return detailMapper.selectList(
                new LambdaQueryWrapper<PyPayrollDetail>()
                        .eq(PyPayrollDetail::getRunId, run.getId()));
    }

    /**
     * 获取最新的社保/公积金费率配置。
     */
    private PySocialInsuranceRate getLatestSIRate() {
        return siRateMapper.selectOne(
                new LambdaQueryWrapper<PySocialInsuranceRate>()
                        .last("LIMIT 1"));
    }

    /**
     * 将Workbook转为字节数组。
     */
    private byte[] toBytes(Workbook wb) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            wb.write(bos);
            return bos.toByteArray();
        }
    }
}
