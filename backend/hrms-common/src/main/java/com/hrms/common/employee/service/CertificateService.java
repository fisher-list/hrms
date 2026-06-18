package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.employee.dto.CertificateCreateDto;
import com.hrms.common.employee.dto.CertificateVo;
import com.hrms.common.employee.entity.HrCertificate;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrCertificateMapper;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.entity.PyCompensationMaster;
import com.hrms.common.payroll.mapper.PyCompensationMasterMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 证明开具服务 —— 在职证明/收入证明自助申请+审批+PDF生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final HrCertificateMapper certificateMapper;
    private final HrEmployeeMapper employeeMapper;
    private final PyCompensationMasterMapper compensationMapper;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;

    /**
     * 员工自助申请证明。
     */
    @Transactional
    public HrCertificate apply(Long employeeId, CertificateCreateDto dto) {
        // 校验员工存在
        HrEmployee emp = employeeMapper.selectById(employeeId);
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + employeeId);
        }

        // 校验证明类型
        if (!"EMPLOYMENT".equals(dto.getType()) && !"INCOME".equals(dto.getType())) {
            throw new BizException(BizCode.BAD_REQUEST, "不支持的证明类型: " + dto.getType());
        }

        // 收入证明需要日期范围
        if ("INCOME".equals(dto.getType())) {
            if (dto.getIncomeStartDate() == null || dto.getIncomeEndDate() == null) {
                throw new BizException(BizCode.BAD_REQUEST, "收入证明需要提供起止日期");
            }
        }

        // 创建证明申请
        HrCertificate cert = new HrCertificate();
        cert.setEmployeeId(employeeId);
        cert.setType(dto.getType());
        cert.setPurpose(dto.getPurpose());
        cert.setCopies(dto.getCopies() != null ? dto.getCopies() : 1);
        cert.setIncomeStartDate(dto.getIncomeStartDate());
        cert.setIncomeEndDate(dto.getIncomeEndDate());
        cert.setStatus("PENDING");
        certificateMapper.insert(cert);

        // 启动审批流程
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("certificateId", cert.getId());
            payload.put("type", cert.getType());
            payload.put("employeeId", employeeId);
            payload.put("purpose", cert.getPurpose());

            Long instanceId = approvalService.start(
                    "CERTIFICATE_APPROVAL",
                    String.valueOf(cert.getId()),
                    "CERTIFICATE",
                    objectMapper.writeValueAsString(payload),
                    employeeId);

            cert.setApprovalInstanceId(instanceId);
            certificateMapper.updateById(cert);
        } catch (Exception e) {
            log.warn("启动证明审批流程失败，证明申请已创建但未进入审批: {}", e.getMessage());
        }

        log.info("员工{}申请{}证明，用途: {}", employeeId, dto.getType(), dto.getPurpose());
        return cert;
    }

    /**
     * 审批通过（由审批事件回调）。
     */
    @Transactional
    public void approve(Long certificateId) {
        HrCertificate cert = certificateMapper.selectById(certificateId);
        if (cert == null) {
            throw new BizException(BizCode.BAD_REQUEST, "证明申请不存在: " + certificateId);
        }
        cert.setStatus("APPROVED");
        certificateMapper.updateById(cert);
    }

    /**
     * 审批拒绝（由审批事件回调）。
     */
    @Transactional
    public void reject(Long certificateId, String reason) {
        HrCertificate cert = certificateMapper.selectById(certificateId);
        if (cert == null) {
            throw new BizException(BizCode.BAD_REQUEST, "证明申请不存在: " + certificateId);
        }
        cert.setStatus("REJECTED");
        cert.setRejectReason(reason);
        certificateMapper.updateById(cert);
    }

    /**
     * 签发证明（审批通过后生成PDF内容）。
     */
    @Transactional
    public byte[] issue(Long certificateId) {
        HrCertificate cert = certificateMapper.selectById(certificateId);
        if (cert == null) {
            throw new BizException(BizCode.BAD_REQUEST, "证明申请不存在: " + certificateId);
        }
        if (!"APPROVED".equals(cert.getStatus())) {
            throw new BizException(BizCode.APPRAISAL_INVALID_STATE, "仅已审批的证明可签发: " + cert.getStatus());
        }

        cert.setStatus("ISSUED");
        cert.setIssuedAt(LocalDateTime.now());
        certificateMapper.updateById(cert);

        // 生成证明内容（HTML格式，可由前端转换为PDF）
        return generateCertificateContent(cert);
    }

    /**
     * 查询员工的证明申请列表。
     */
    public List<CertificateVo> list(Long employeeId, String status) {
        LambdaQueryWrapper<HrCertificate> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(HrCertificate::getEmployeeId, employeeId);
        }
        if (status != null) {
            wrapper.eq(HrCertificate::getStatus, status);
        }
        wrapper.orderByDesc(HrCertificate::getCreatedAt);

        return certificateMapper.selectList(wrapper).stream().map(this::toVo).toList();
    }

    /**
     * 获取证明详情。
     */
    public CertificateVo getById(Long id) {
        HrCertificate cert = certificateMapper.selectById(id);
        if (cert == null) {
            throw new BizException(BizCode.BAD_REQUEST, "证明申请不存在: " + id);
        }
        return toVo(cert);
    }

    /**
     * 生成证明内容（HTML格式，用于PDF渲染）。
     */
    private byte[] generateCertificateContent(HrCertificate cert) {
        HrEmployee emp = employeeMapper.selectById(cert.getEmployeeId());
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + cert.getEmployeeId());
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        String issuedDate = cert.getIssuedAt().format(dtf);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:SimSun,serif;padding:60px;}");
        html.append("h1{text-align:center;font-size:24px;}");
        html.append(".content{font-size:14px;line-height:2;}");
        html.append(".footer{text-align:right;margin-top:40px;}");
        html.append("</style></head><body>");

        if ("EMPLOYMENT".equals(cert.getType())) {
            html.append("<h1>在职证明</h1>");
            html.append("<div class='content'>");
            html.append("<p>").append(emp.getName()).append("，性别").append("M".equals(emp.getGender()) ? "男" : "女");
            html.append("，身份证号码：***************");
            html.append("，自").append(emp.getHireDate().format(dtf));
            html.append("起在我单位工作，现担任职务。</p>");
            html.append("<p>特此证明。</p>");
            html.append("<p>用途：").append(cert.getPurpose()).append("</p>");
            html.append("</div>");
        } else {
            // INCOME
            PyCompensationMaster comp = compensationMapper.selectOne(
                    new LambdaQueryWrapper<PyCompensationMaster>()
                            .eq(PyCompensationMaster::getEmployeeId, cert.getEmployeeId())
                            .orderByDesc(PyCompensationMaster::getEffectiveDate)
                            .last("LIMIT 1"));
            BigDecimal monthlyIncome = comp != null ? comp.getBaseSalary() : BigDecimal.ZERO;

            html.append("<h1>收入证明</h1>");
            html.append("<div class='content'>");
            html.append("<p>").append(emp.getName()).append("，系我单位员工");
            html.append("，自").append(emp.getHireDate().format(dtf)).append("起在我单位工作。</p>");
            html.append("<p>其在").append(cert.getIncomeStartDate().format(dtf));
            html.append("至").append(cert.getIncomeEndDate().format(dtf));
            html.append("期间的月收入为人民币").append(monthlyIncome).append("元。</p>");
            html.append("<p>特此证明。</p>");
            html.append("<p>用途：").append(cert.getPurpose()).append("</p>");
            html.append("</div>");
        }

        html.append("<div class='footer'>");
        html.append("<p>开具日期：").append(issuedDate).append("</p>");
        html.append("<p>（单位公章）</p>");
        html.append("</div>");
        html.append("</body></html>");

        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    private CertificateVo toVo(HrCertificate cert) {
        CertificateVo vo = new CertificateVo();
        vo.setId(cert.getId());
        vo.setEmployeeId(cert.getEmployeeId());
        vo.setType(cert.getType());
        vo.setPurpose(cert.getPurpose());
        vo.setCopies(cert.getCopies());
        vo.setStatus(cert.getStatus());
        vo.setApprovalInstanceId(cert.getApprovalInstanceId());
        vo.setIssuedAt(cert.getIssuedAt());
        vo.setIncomeStartDate(cert.getIncomeStartDate());
        vo.setIncomeEndDate(cert.getIncomeEndDate());
        vo.setRejectReason(cert.getRejectReason());

        HrEmployee emp = employeeMapper.selectById(cert.getEmployeeId());
        if (emp != null) {
            vo.setEmployeeName(emp.getName());
            vo.setEmpNo(emp.getEmpNo());
        }
        return vo;
    }
}
