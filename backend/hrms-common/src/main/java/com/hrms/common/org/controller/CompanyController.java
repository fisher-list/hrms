package com.hrms.common.org.controller;

import com.hrms.common.api.R;
import com.hrms.common.org.dto.CompanyDto;
import com.hrms.common.org.entity.Company;
import com.hrms.common.org.service.CompanyService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for company (singleton).
 */
@Tag(name = "Company", description = "Company management (singleton)")
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Get company info")
    @GetMapping
    @HasPermission("org:view")
    public R<Company> get() {
        return R.ok(companyService.getSingle());
    }

    @Operation(summary = "Update company info")
    @PutMapping
    @HasPermission("org:edit")
    public R<Company> update(@Valid @RequestBody CompanyDto dto) {
        Company existing = companyService.getSingle();
        if (existing == null) {
            return R.ok(companyService.create(dto));
        }
        return R.ok(companyService.update(existing.getId(), dto));
    }
}
