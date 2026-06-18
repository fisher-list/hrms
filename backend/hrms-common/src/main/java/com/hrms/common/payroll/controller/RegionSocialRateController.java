package com.hrms.common.payroll.controller;

import com.hrms.common.api.R;
import com.hrms.common.payroll.entity.PyRegionSocialRate;
import com.hrms.common.payroll.service.RegionSocialRateService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 多地区社保公积金政策管理接口。
 */
@Tag(name = "RegionSocialRate", description = "多地区社保公积金政策管理")
@RestController
@RequestMapping("/api/payroll/region-social-rates")
@RequiredArgsConstructor
public class RegionSocialRateController {

    private final RegionSocialRateService regionSocialRateService;

    @Operation(summary = "创建地区社保政策")
    @PostMapping
    @HasPermission("py:region-rate:edit")
    public R<PyRegionSocialRate> create(@Valid @RequestBody PyRegionSocialRate rate) {
        return R.ok(regionSocialRateService.create(rate));
    }

    @Operation(summary = "更新地区社保政策")
    @PutMapping("/{id}")
    @HasPermission("py:region-rate:edit")
    public R<PyRegionSocialRate> update(@PathVariable Long id, @RequestBody PyRegionSocialRate rate) {
        return R.ok(regionSocialRateService.update(id, rate));
    }

    @Operation(summary = "根据城市查询社保政策")
    @GetMapping("/city/{city}")
    @HasPermission("py:region-rate:list")
    public R<PyRegionSocialRate> getByCity(@PathVariable String city) {
        return R.ok(regionSocialRateService.getByCity(city));
    }

    @Operation(summary = "查询所有启用的地区社保政策")
    @GetMapping
    @HasPermission("py:region-rate:list")
    public R<List<PyRegionSocialRate>> listAll() {
        return R.ok(regionSocialRateService.listAll());
    }

    @Operation(summary = "禁用地区社保政策")
    @DeleteMapping("/{id}")
    @HasPermission("py:region-rate:edit")
    public R<Void> disable(@PathVariable Long id) {
        regionSocialRateService.disable(id);
        return R.ok();
    }
}
