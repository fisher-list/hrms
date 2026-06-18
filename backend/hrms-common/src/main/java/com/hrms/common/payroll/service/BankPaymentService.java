package com.hrms.common.payroll.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.entity.HrEmployeeBankAccount;
import com.hrms.common.employee.mapper.HrEmployeeBankAccountMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.dto.BankPaymentCreateDto;
import com.hrms.common.payroll.entity.*;
import com.hrms.common.payroll.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 银行代发服务。
 *
 * <p>根据锁定的薪酬运行批次生成银行代发文件，支持多种银行格式：
 * <ul>
 *   <li>GENERAL_CSV - 通用CSV格式</li>
 *   <li>ICBC - 工商银行专用格式</li>
 *   <li>CCB - 建设银行专用格式</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankPaymentService {

    private final PyBankPaymentBatchMapper batchMapper;
    private final PyBankPaymentDetailMapper detailMapper;
    private final PyPayrollRunMapper runMapper;
    private final PyPayrollDetailMapper payrollDetailMapper;
    private final HrEmployeeBankAccountMapper bankAccountMapper;

    /** 批次号日期格式 */
    private static final DateTimeFormatter BATCH_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 创建银行代发批次并生成代发明细。
     *
     * @param dto 银行代发创建DTO
     * @return 代发批次信息
     */
    @Transactional
    public PyBankPaymentBatch createBatch(BankPaymentCreateDto dto) {
        // 验证薪酬运行必须是LOCKED状态
        PyPayrollRun run = runMapper.selectById(dto.getRunId());
        if (run == null) {
            throw new BizException(BizCode.BAD_REQUEST, "薪酬运行不存在");
        }
        if (!"LOCKED".equals(run.getStatus())) {
            throw new BizException(BizCode.PAYROLL_RUN_NOT_LOCKED,
                    "只有LOCKED状态的薪酬运行才能生成银行代发，当前状态：" + run.getStatus());
        }

        // 检查是否已为该运行生成过同类型代发批次
        Long existingCount = batchMapper.selectCount(
                new LambdaQueryWrapper<PyBankPaymentBatch>()
                        .eq(PyBankPaymentBatch::getRunId, dto.getRunId())
                        .eq(PyBankPaymentBatch::getBankType, dto.getBankType()));
        if (existingCount > 0) {
            throw new BizException(BizCode.PAYROLL_BANK_PAYMENT_ERROR,
                    "该薪酬运行已生成过此银行类型的代发批次");
        }

        // 生成批次号：BP + 日期 + 序号
        String batchNo = generateBatchNo();

        // 获取薪酬明细
        List<PyPayrollDetail> payrollDetails = payrollDetailMapper.selectList(
                new LambdaQueryWrapper<PyPayrollDetail>()
                        .eq(PyPayrollDetail::getRunId, dto.getRunId())
                        .eq(PyPayrollDetail::getIsException, false));

        // 批量加载银行账户信息
        List<Long> empIds = payrollDetails.stream()
                .map(PyPayrollDetail::getEmployeeId)
                .collect(Collectors.toList());
        Map<Long, HrEmployeeBankAccount> bankAccountMap = loadBankAccounts(empIds);

        // 创建批次
        PyBankPaymentBatch batch = new PyBankPaymentBatch();
        batch.setRunId(dto.getRunId());
        batch.setBatchNo(batchNo);
        batch.setBankType(dto.getBankType());
        batch.setPayerAccountName(dto.getPayerAccountName());
        batch.setPayerAccountNo(dto.getPayerAccountNo());
        batch.setPayerBankName(dto.getPayerBankName());
        batch.setEmployeeCount(payrollDetails.size());
        batch.setStatus("GENERATED");
        batch.setRemark(dto.getRemark());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PyBankPaymentDetail> details = new ArrayList<>();

        for (PyPayrollDetail pd : payrollDetails) {
            HrEmployeeBankAccount bankAccount = bankAccountMap.get(pd.getEmployeeId());

            PyBankPaymentDetail detail = new PyBankPaymentDetail();
            detail.setEmployeeId(pd.getEmployeeId());
            detail.setEmployeeName(pd.getEmployeeName());
            detail.setEmpNo(pd.getEmpNo());
            detail.setAmount(pd.getNetPay());
            detail.setPurpose("工资发放");

            if (bankAccount != null) {
                detail.setBankName(bankAccount.getBankName());
                detail.setAccountNo(bankAccount.getAccountNoEnc()); // 实际应解密
                detail.setAccountName(bankAccount.getAccountName());
            } else {
                detail.setBankName("未知");
                detail.setAccountNo("未知");
                detail.setAccountName(pd.getEmployeeName());
            }

            details.add(detail);
            totalAmount = totalAmount.add(pd.getNetPay());
        }

        batch.setTotalAmount(totalAmount.setScale(4, RoundingMode.HALF_UP));
        batchMapper.insert(batch);

        // 批量保存明细
        for (PyBankPaymentDetail detail : details) {
            detail.setBatchId(batch.getId());
            detailMapper.insert(detail);
        }

        return batch;
    }

    /**
     * 生成银行代发文件内容（字符串）。
     * 根据银行类型生成不同格式。
     *
     * @param batchId 批次ID
     * @return 文件内容字符串
     */
    public String generateFileContent(Long batchId) {
        PyBankPaymentBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException(BizCode.BAD_REQUEST, "代发批次不存在");
        }

        List<PyBankPaymentDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<PyBankPaymentDetail>()
                        .eq(PyBankPaymentDetail::getBatchId, batchId)
                        .orderByAsc(PyBankPaymentDetail::getEmpNo));

        return switch (batch.getBankType()) {
            case "ICBC" -> generateICBCFormat(batch, details);
            case "CCB" -> generateCCBFormat(batch, details);
            default -> generateGeneralCSV(batch, details);
        };
    }

    /**
     * 查询代发批次列表。
     */
    public List<PyBankPaymentBatch> listBatches(Long runId) {
        LambdaQueryWrapper<PyBankPaymentBatch> wrapper = new LambdaQueryWrapper<>();
        if (runId != null) {
            wrapper.eq(PyBankPaymentBatch::getRunId, runId);
        }
        wrapper.orderByDesc(PyBankPaymentBatch::getCreatedAt);
        return batchMapper.selectList(wrapper);
    }

    /**
     * 查询代发批次明细。
     */
    public List<PyBankPaymentDetail> getBatchDetails(Long batchId) {
        return detailMapper.selectList(
                new LambdaQueryWrapper<PyBankPaymentDetail>()
                        .eq(PyBankPaymentDetail::getBatchId, batchId)
                        .orderByAsc(PyBankPaymentDetail::getEmpNo));
    }

    /**
     * 标记批次为已导出。
     */
    @Transactional
    public PyBankPaymentBatch markExported(Long batchId) {
        PyBankPaymentBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException(BizCode.BAD_REQUEST, "代发批次不存在");
        }
        batch.setStatus("EXPORTED");
        batchMapper.updateById(batch);
        return batch;
    }

    /**
     * 标记批次为已确认（银行已处理）。
     */
    @Transactional
    public PyBankPaymentBatch markConfirmed(Long batchId) {
        PyBankPaymentBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException(BizCode.BAD_REQUEST, "代发批次不存在");
        }
        batch.setStatus("CONFIRMED");
        batchMapper.updateById(batch);
        return batch;
    }

    // ---- 私有方法 ----

    /**
     * 生成批次号：BP + 年月日 + 4位序号
     */
    private String generateBatchNo() {
        String dateStr = LocalDate.now().format(BATCH_DATE_FMT);
        // 查询今日已生成的批次数量
        Long count = batchMapper.selectCount(
                new LambdaQueryWrapper<PyBankPaymentBatch>()
                        .likeRight(PyBankPaymentBatch::getBatchNo, "BP" + dateStr));
        return "BP" + dateStr + String.format("%04d", count + 1);
    }

    /**
     * 批量加载员工主银行账户。
     */
    private Map<Long, HrEmployeeBankAccount> loadBankAccounts(List<Long> empIds) {
        if (empIds == null || empIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrEmployeeBankAccount> accounts = bankAccountMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeBankAccount>()
                        .in(HrEmployeeBankAccount::getEmployeeId, empIds)
                        .eq(HrEmployeeBankAccount::getIsPrimary, true));
        Map<Long, HrEmployeeBankAccount> result = new HashMap<>();
        for (HrEmployeeBankAccount account : accounts) {
            result.putIfAbsent(account.getEmployeeId(), account);
        }
        return result;
    }

    /**
     * 生成通用CSV格式。
     * 格式：序号,员工编号,员工姓名,银行名称,账号,金额,用途
     */
    private String generateGeneralCSV(PyBankPaymentBatch batch,
                                       List<PyBankPaymentDetail> details) {
        StringBuilder sb = new StringBuilder();
        // 头部信息
        sb.append("批次号:,").append(batch.getBatchNo()).append("\n");
        sb.append("付款账户:,").append(batch.getPayerAccountName()).append("\n");
        sb.append("付款账号:,").append(batch.getPayerAccountNo()).append("\n");
        sb.append("总人数:,").append(batch.getEmployeeCount()).append("\n");
        sb.append("总金额:,").append(batch.getTotalAmount()).append("\n");
        sb.append("\n");
        // 表头
        sb.append("序号,员工编号,员工姓名,银行名称,账号,金额,用途\n");
        // 数据行
        int seq = 0;
        for (PyBankPaymentDetail d : details) {
            seq++;
            sb.append(seq).append(",")
                    .append(nullSafe(d.getEmpNo())).append(",")
                    .append(nullSafe(d.getEmployeeName())).append(",")
                    .append(nullSafe(d.getBankName())).append(",")
                    .append(nullSafe(d.getAccountNo())).append(",")
                    .append(d.getAmount()).append(",")
                    .append(nullSafe(d.getPurpose())).append("\n");
        }
        return sb.toString();
    }

    /**
     * 生成工商银行(ICBC)格式。
     * 格式：企业账号|户名|金额|收款人账号|收款人姓名|收款行名称|摘要
     */
    private String generateICBCFormat(PyBankPaymentBatch batch,
                                       List<PyBankPaymentDetail> details) {
        StringBuilder sb = new StringBuilder();
        // 工行头信息
        sb.append("ICBC代发工资文件\n");
        sb.append("企业账号:").append(batch.getPayerAccountNo()).append("\n");
        sb.append("企业名称:").append(batch.getPayerAccountName()).append("\n");
        sb.append("总笔数:").append(batch.getEmployeeCount()).append("\n");
        sb.append("总金额:").append(batch.getTotalAmount()).append("\n");
        sb.append("========================================\n");
        // 工行数据行：|分隔
        for (PyBankPaymentDetail d : details) {
            sb.append(batch.getPayerAccountNo()).append("|")
                    .append(batch.getPayerAccountName()).append("|")
                    .append(d.getAmount()).append("|")
                    .append(nullSafe(d.getAccountNo())).append("|")
                    .append(nullSafe(d.getAccountName())).append("|")
                    .append(nullSafe(d.getBankName())).append("|")
                    .append("工资").append("\n");
        }
        return sb.toString();
    }

    /**
     * 生成建设银行(CCB)格式。
     * 格式：付款账号,付款户名,收款账号,收款户名,收款银行,金额,备注
     */
    private String generateCCBFormat(PyBankPaymentBatch batch,
                                      List<PyBankPaymentDetail> details) {
        StringBuilder sb = new StringBuilder();
        // 建行头信息
        sb.append("CCB代发工资文件\n");
        sb.append("付款账号:,").append(batch.getPayerAccountNo()).append("\n");
        sb.append("付款户名:,").append(batch.getPayerAccountName()).append("\n");
        sb.append("总笔数:,").append(batch.getEmployeeCount()).append("\n");
        sb.append("总金额:,").append(batch.getTotalAmount()).append("\n");
        sb.append("\n");
        // 建行表头
        sb.append("付款账号,付款户名,收款账号,收款户名,收款银行,金额,备注\n");
        // 数据行
        for (PyBankPaymentDetail d : details) {
            sb.append(batch.getPayerAccountNo()).append(",")
                    .append(batch.getPayerAccountName()).append(",")
                    .append(nullSafe(d.getAccountNo())).append(",")
                    .append(nullSafe(d.getAccountName())).append(",")
                    .append(nullSafe(d.getBankName())).append(",")
                    .append(d.getAmount()).append(",")
                    .append("工资").append("\n");
        }
        return sb.toString();
    }

    /**
     * 安全处理null字符串。
     */
    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
