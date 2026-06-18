package com.hrms.common.recruit.controller;

import com.hrms.common.api.R;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.recruit.dto.InterviewCreateDto;
import com.hrms.common.recruit.entity.RcInterview;
import com.hrms.common.recruit.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Interview", description = "Interview scheduling")
@RestController
@RequestMapping("/api/recruit/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "Schedule interview")
    @PostMapping
    @HasPermission("rc:interview:edit")
    public R<RcInterview> create(@Valid @RequestBody InterviewCreateDto dto) {
        return R.ok(interviewService.create(dto));
    }

    @Operation(summary = "List interviews by candidate")
    @GetMapping
    @HasPermission("rc:interview:list")
    public R<List<RcInterview>> listByCandidate(@RequestParam Long candidateId) {
        return R.ok(interviewService.listByCandidate(candidateId));
    }

    @Operation(summary = "Record interview result")
    @PostMapping("/{id}/result")
    @HasPermission("rc:interview:edit")
    public R<Void> recordResult(@PathVariable Long id, @RequestParam String result) {
        interviewService.recordResult(id, result);
        return R.ok();
    }
}
