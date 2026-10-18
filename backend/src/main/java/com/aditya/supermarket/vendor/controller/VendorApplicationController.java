package com.aditya.supermarket.vendor.controller;

import com.aditya.supermarket.common.security.CurrentUser;
import com.aditya.supermarket.vendor.dto.VendorApplicationRequest;
import com.aditya.supermarket.vendor.dto.VendorApplicationResponse;
import com.aditya.supermarket.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A signed-in customer's own vendor application. */
@Tag(name = "Vendor applications")
@RestController
@RequestMapping("/api/v1/vendor-applications")
@RequiredArgsConstructor
public class VendorApplicationController {

    private final VendorService vendorService;

    @Operation(summary = "Apply to become a vendor")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendorApplicationResponse apply(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody VendorApplicationRequest request) {
        return vendorService.apply(CurrentUser.id(jwt), request);
    }

    @Operation(summary = "Get my vendor application")
    @GetMapping("/me")
    public VendorApplicationResponse getMine(@AuthenticationPrincipal Jwt jwt) {
        return vendorService.getMine(CurrentUser.id(jwt));
    }

    @Operation(summary = "Edit or resubmit my vendor application")
    @PutMapping("/me")
    public VendorApplicationResponse updateMine(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody VendorApplicationRequest request) {
        return vendorService.updateMine(CurrentUser.id(jwt), request);
    }
}
