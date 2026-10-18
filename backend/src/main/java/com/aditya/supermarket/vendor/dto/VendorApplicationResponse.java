package com.aditya.supermarket.vendor.dto;

import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.vendor.entity.Vendor;
import com.aditya.supermarket.vendor.entity.VendorStatus;
import java.time.Instant;

/** A vendor application as seen by the applicant and by admins. */
public record VendorApplicationResponse(
    Long id,
    String storeName,
    String storeDescription,
    String contactPhone,
    String businessAddress,
    String gstNumber,
    VendorStatus status,
    String rejectionReason,
    Instant reviewedAt,
    Instant createdAt,
    Instant updatedAt,
    Applicant applicant) {

    /** Minimal applicant details; never exposes anything sensitive. */
    public record Applicant(Long id, String email, String name) {
    }

    public static VendorApplicationResponse from(Vendor vendor) {
        User user = vendor.getUser();
        return new VendorApplicationResponse(
            vendor.getId(),
            vendor.getStoreName(),
            vendor.getStoreDescription(),
            vendor.getContactPhone(),
            vendor.getBusinessAddress(),
            vendor.getGstNumber(),
            vendor.getStatus(),
            vendor.getRejectionReason(),
            vendor.getReviewedAt(),
            vendor.getCreatedAt(),
            vendor.getUpdatedAt(),
            new Applicant(user.getId(), user.getEmail(), user.getFirstName() + " " + user.getLastName()));
    }
}
