package com.aditya.supermarket.notification.event;

/** Published when an admin rejects a vendor application; carries the reason the admin gave. */
public record VendorApplicationRejectedEvent(
    Long vendorId, Long applicantId, String applicantEmail, String applicantFirstName,
    String storeName, String reason) {
}
