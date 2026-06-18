package com.hrms.common.recruit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.recruit.dto.CandidateCreateDto;
import com.hrms.common.recruit.entity.RcCandidate;
import com.hrms.common.recruit.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Candidate", description = "Candidate management")
@RestController
@RequestMapping("/api/recruit/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "Create candidate")
    @PostMapping
    @HasPermission("rc:candidate:edit")
    public R<RcCandidate> create(@Valid @RequestBody CandidateCreateDto dto) {
        return R.ok(candidateService.create(dto));
    }

    @Operation(summary = "Get candidate by ID")
    @GetMapping("/{id}")
    @HasPermission("rc:candidate:list")
    public R<RcCandidate> getById(@PathVariable Long id) {
        return R.ok(candidateService.getById(id));
    }

    @Operation(summary = "List all candidates")
    @GetMapping
    @HasPermission("rc:candidate:list")
    public R<IPage<RcCandidate>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(candidateService.list(new Page<>(current, size)));
    }
}
