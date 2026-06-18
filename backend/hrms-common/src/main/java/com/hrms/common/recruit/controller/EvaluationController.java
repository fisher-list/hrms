package com.hrms.common.recruit.controller;

import com.hrms.common.api.R;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.recruit.dto.EvaluationCreateDto;
import com.hrms.common.recruit.entity.RcEvaluation;
import com.hrms.common.recruit.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Evaluation", description = "Interview evaluation")
@RestController
@RequestMapping("/api/recruit/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "Create evaluation (append-only)")
    @PostMapping
    @HasPermission("rc:evaluation:edit")
    public R<RcEvaluation> create(@Valid @RequestBody EvaluationCreateDto dto) {
        return R.ok(evaluationService.create(dto));
    }

    @Operation(summary = "List evaluations by interview")
    @GetMapping
    @HasPermission("rc:evaluation:edit")
    public R<List<RcEvaluation>> listByInterview(@RequestParam Long interviewId) {
        return R.ok(evaluationService.listByInterview(interviewId));
    }
}
