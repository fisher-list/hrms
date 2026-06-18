package com.hrms.common.payroll.controller;

import com.hrms.common.api.R;
import com.hrms.common.payroll.dto.PayrollRunCreateDto;
import com.hrms.common.payroll.dto.PayrollRunReverseDto;
import com.hrms.common.payroll.entity.PyPayrollDetail;
import com.hrms.common.payroll.entity.PyPayrollRun;
import com.hrms.common.payroll.service.PayrollService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "PayrollRun", description = "Payroll run management")
@RestController
@RequestMapping("/api/payroll/runs")
@RequiredArgsConstructor
public class PayrollRunController {

    private final PayrollService payrollService;

    @Operation(summary = "Create payroll run")
    @PostMapping
    @HasPermission("py:run:create")
    public R<PyPayrollRun> create(@Valid @RequestBody PayrollRunCreateDto dto) {
        return R.ok(payrollService.createRun(dto.getPeriodId()));
    }

    @Operation(summary = "Calculate payroll run")
    @PostMapping("/{id}/calculate")
    @HasPermission("py:run:calculate")
    public R<List<Long>> calculate(@PathVariable Long id) {
        return R.ok(payrollService.calculate(id));
    }

    @Operation(summary = "Lock payroll run")
    @PostMapping("/{id}/lock")
    @HasPermission("py:run:lock")
    public R<PyPayrollRun> lock(@PathVariable Long id) {
        return R.ok(payrollService.lockRun(id));
    }

    @Operation(summary = "Reverse payroll run")
    @PostMapping("/{id}/reverse")
    @HasPermission("py:run:reverse")
    public R<PyPayrollRun> reverse(@PathVariable Long id) {
        return R.ok(payrollService.reverseRun(id));
    }

    @Operation(summary = "List payroll runs")
    @GetMapping
    @HasPermission("py:run:list")
    public R<List<PyPayrollRun>> list(@RequestParam(required = false) Long periodId) {
        return R.ok(payrollService.listRuns(periodId));
    }

    @Operation(summary = "Get payroll run detail")
    @GetMapping("/{id}")
    @HasPermission("py:run:list")
    public R<List<PyPayrollDetail>> getRunDetails(@PathVariable Long id) {
        return R.ok(payrollService.getRunDetails(id));
    }
}
