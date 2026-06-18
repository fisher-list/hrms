package com.hrms.common.performance.controller;

import com.hrms.common.api.R;
import com.hrms.common.performance.entity.PfScoringItem;
import com.hrms.common.performance.entity.PfTemplate;
import com.hrms.common.performance.service.TemplateService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "PerformanceTemplate", description = "Performance template management")
@RestController
@RequestMapping("/api/performance/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @Operation(summary = "Create performance template")
    @PostMapping
    @HasPermission("pf:template:edit")
    public R<PfTemplate> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        List<PfScoringItem> items = (List<PfScoringItem>) body.get("scoringItems");
        return R.ok(templateService.create(name, description, items));
    }

    @Operation(summary = "List templates")
    @GetMapping
    @HasPermission("pf:template:list")
    public R<List<PfTemplate>> list() {
        return R.ok(templateService.list());
    }

    @Operation(summary = "Get template by ID with scoring items")
    @GetMapping("/{id}")
    @HasPermission("pf:template:list")
    public R<Map<String, Object>> getById(@PathVariable Long id) {
        PfTemplate template = templateService.getById(id);
        List<PfScoringItem> items = templateService.getScoringItems(id);
        Map<String, Object> result = new HashMap<>();
        result.put("template", template);
        result.put("scoringItems", items);
        return R.ok(result);
    }
}
