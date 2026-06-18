package com.hrms.common.org.controller;

import com.hrms.common.api.R;
import com.hrms.common.org.dto.JobDto;
import com.hrms.common.org.entity.Job;
import com.hrms.common.org.service.JobService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for job CRUD.
 */
@Tag(name = "Jobs", description = "Job family / grade management")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "List all jobs")
    @GetMapping
    @HasPermission("org:view")
    public R<List<Job>> list() {
        return R.ok(jobService.list());
    }

    @Operation(summary = "Get job by ID")
    @GetMapping("/{id}")
    @HasPermission("org:view")
    public R<Job> getById(@PathVariable Long id) {
        return R.ok(jobService.getById(id));
    }

    @Operation(summary = "Create a job")
    @PostMapping
    @HasPermission("org:create")
    public R<Job> create(@Valid @RequestBody JobDto dto) {
        return R.ok(jobService.create(dto));
    }

    @Operation(summary = "Update a job")
    @PutMapping("/{id}")
    @HasPermission("org:edit")
    public R<Job> update(@PathVariable Long id, @Valid @RequestBody JobDto dto) {
        return R.ok(jobService.update(id, dto));
    }

    @Operation(summary = "Delete a job")
    @DeleteMapping("/{id}")
    @HasPermission("org:delete")
    public R<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return R.ok();
    }
}
