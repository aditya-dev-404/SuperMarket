package com.aditya.supermarket.vendor.controller;

import com.aditya.supermarket.common.security.CurrentUser;
import com.aditya.supermarket.vendor.dto.RejectRequest;
import com.aditya.supermarket.vendor.dto.VendorApplicationResponse;
import com.aditya.supermarket.vendor.entity.VendorStatus;
import com.aditya.supermarket.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin review of vendor applications. Secured by the /api/v1/admin/** rule in SecurityConfig. */
@Tag(name = "Admin - Vendor applications")
@RestController
@RequestMapping("/api/v1/admin/vendor-applications")
@RequiredArgsConstructor
public class AdminVendorController {

    private final VendorService vendorService;

    @Operation(summary = "List vendor applications, optionally filtered by status")
    @GetMapping
    public PagedModel<VendorApplicationResponse> list(
        @RequestParam(required = false) VendorStatus status,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return vendorService.list(status, pageable);
    }

    @Operation(summary = "Get one vendor application")
    @GetMapping("/{id}")
    public VendorApplicationResponse get(@PathVariable Long id) {
        return vendorService.get(id);
    }

    @Operation(summary = "Approve a vendor application (user becomes a VENDOR)")
    @PostMapping("/{id}/approve")
    public VendorApplicationResponse approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return vendorService.approve(id, CurrentUser.id(jwt));
    }

    @Operation(summary = "Reject a vendor application with a reason")
    @PostMapping("/{id}/reject")
    public VendorApplicationResponse reject(@PathVariable Long id,
                                            @Valid @RequestBody RejectRequest request, @AuthenticationPrincipal Jwt jwt) {
        return vendorService.reject(id, request.reason(), CurrentUser.id(jwt));
    }
}
