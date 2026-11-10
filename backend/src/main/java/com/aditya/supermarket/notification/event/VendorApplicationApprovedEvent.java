package com.aditya.supermarket.notification.event;

/** Published when an admin approves a vendor application. */
public record VendorApplicationApprovedEvent(
    Long vendorId, Long applicantId, String applicantEmail, String applicantFirstName, String storeName) {
}
