package com.hrms.common.employee.controller;

import com.hrms.common.api.R;
import com.hrms.common.employee.dto.CertificateCreateDto;
import com.hrms.common.employee.dto.CertificateVo;
import com.hrms.common.employee.entity.HrCertificate;
import com.hrms.common.employee.service.CertificateService;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 证明开具控制器 —— 在职证明/收入证明自助申请+审批+PDF生成。
 */
@Tag(name = "Certificate", description = "证明开具")
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @Operation(summary = "员工自助申请证明")
    @PostMapping("/apply")
    @HasPermission("cert:apply")
    public R<HrCertificate> apply(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody CertificateCreateDto dto) {
        return R.ok(certificateService.apply(loginUser.requireEmployeeId(), dto));
    }

    @Operation(summary = "查询我的证明申请列表")
    @GetMapping("/my")
    @HasPermission("cert:apply")
    public R<List<CertificateVo>> myApplications(@AuthenticationPrincipal LoginUser loginUser,
                                                  @RequestParam(required = false) String status) {
        return R.ok(certificateService.list(loginUser.requireEmployeeId(), status));
    }

    @Operation(summary = "查询证明申请列表（管理员）")
    @GetMapping
    @HasPermission("cert:list")
    public R<List<CertificateVo>> list(@RequestParam(required = false) Long employeeId,
                                        @RequestParam(required = false) String status) {
        return R.ok(certificateService.list(employeeId, status));
    }

    @Operation(summary = "获取证明详情")
    @GetMapping("/{id}")
    @HasPermission("cert:list")
    public R<CertificateVo> getById(@PathVariable Long id) {
        return R.ok(certificateService.getById(id));
    }

    @Operation(summary = "签发证明（审批通过后）")
    @PostMapping("/{id}/issue")
    @HasPermission("cert:issue")
    public R<Void> issue(@PathVariable Long id) {
        certificateService.issue(id);
        return R.ok(null);
    }

    @Operation(summary = "下载证明PDF")
    @GetMapping("/{id}/download")
    @HasPermission("cert:apply")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        byte[] content = certificateService.issue(id);
        String fileName = URLEncoder.encode("证明_" + id + ".html", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.setContentLength(content.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(content);
        }
    }
}
