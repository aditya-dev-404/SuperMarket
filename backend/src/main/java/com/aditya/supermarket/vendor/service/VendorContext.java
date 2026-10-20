package com.aditya.supermarket.vendor.service;

import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.security.CurrentUser;
import com.aditya.supermarket.vendor.entity.Vendor;
import com.aditya.supermarket.vendor.entity.VendorStatus;
import com.aditya.supermarket.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Resolves the logged-in user's approved vendor profile. Single place to adapt to your auth setup. */
@Component
@RequiredArgsConstructor
public class VendorContext {

    private final VendorRepository vendorRepository;

    public Vendor requireApprovedVendor() {
        Vendor vendor = vendorRepository.findByUserId(currentUserId())
            .orElseThrow(() -> new BusinessRuleException("No vendor profile found for this account"));
        if (vendor.getStatus() != VendorStatus.APPROVED) {
            throw new BusinessRuleException("Your vendor account is not approved yet");
        }
        return vendor;
    }
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessRuleException("Not authenticated");
        }
        return CurrentUser.id(jwt);
    }
}
