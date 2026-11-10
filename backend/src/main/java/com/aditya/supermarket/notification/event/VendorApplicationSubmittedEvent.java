package com.aditya.supermarket.notification.event;

/**
 * Published when a customer submits a vendor application, or resubmits a rejected one.
 * Triggers a receipt email to the applicant and an alert email to every admin.
 */
public record VendorApplicationSubmittedEvent(
    Long vendorId,
    Long applicantId,
    String applicantEmail,
    String applicantFirstName,
    String applicantFullName,
    String storeName,
    String contactPhone,
    String gstNumber,
    boolean resubmission) {
}
