package com.hrms.common.recruit.controller;

import com.hrms.common.api.R;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.recruit.dto.OfferCreateDto;
import com.hrms.common.recruit.entity.RcOffer;
import com.hrms.common.recruit.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Offer", description = "Offer management")
@RestController
@RequestMapping("/api/recruit/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @Operation(summary = "Create offer")
    @PostMapping
    @HasPermission("rc:offer:edit")
    public R<RcOffer> create(@Valid @RequestBody OfferCreateDto dto) {
        return R.ok(offerService.create(dto));
    }

    @Operation(summary = "Get offer by ID")
    @GetMapping("/{id}")
    @HasPermission("rc:offer:list")
    public R<RcOffer> getById(@PathVariable Long id) {
        return R.ok(offerService.getById(id));
    }

    @Operation(summary = "Submit offer for approval")
    @PostMapping("/{id}/submit")
    @HasPermission("rc:offer:edit")
    public R<Void> submit(@PathVariable Long id) {
        // TODO: get applicantId from security context
        offerService.submit(id, 1L);
        return R.ok();
    }

    @Operation(summary = "Accept offer")
    @PostMapping("/{id}/accept")
    @HasPermission("rc:offer:accept")
    public R<Void> accept(@PathVariable Long id) {
        offerService.accept(id);
        return R.ok();
    }

    @Operation(summary = "Decline offer")
    @PostMapping("/{id}/decline")
    @HasPermission("rc:offer:accept")
    public R<Void> decline(@PathVariable Long id) {
        offerService.decline(id);
        return R.ok();
    }
}
