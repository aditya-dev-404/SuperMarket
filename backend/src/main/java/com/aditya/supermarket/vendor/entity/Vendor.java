package com.aditya.supermarket.vendor.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import com.aditya.supermarket.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A vendor application; once approved it is the vendor's store profile. One per user. */
@Getter
@Entity
@Table(name = "vendors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vendor extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(name = "store_description", length = 1000)
    private String storeDescription;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "business_address", nullable = false, length = 500)
    private String businessAddress;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private VendorStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public Vendor(User user, String storeName, String storeDescription, String contactPhone,
                  String businessAddress, String gstNumber) {
        this.user = user;
        this.status = VendorStatus.PENDING;
        applyDetails(storeName, storeDescription, contactPhone, businessAddress, gstNumber);
    }

    /** Edits the details. A rejected application goes back to PENDING (resubmission). */
    public void updateDetails(String storeName, String storeDescription, String contactPhone,
                              String businessAddress, String gstNumber) {
        applyDetails(storeName, storeDescription, contactPhone, businessAddress, gstNumber);
        if (status == VendorStatus.REJECTED) {
            status = VendorStatus.PENDING;
            rejectionReason = null;
            reviewedBy = null;
            reviewedAt = null;
        }
    }

    public void approve(User admin) {
        this.status = VendorStatus.APPROVED;
        this.rejectionReason = null;
        this.reviewedBy = admin;
        this.reviewedAt = Instant.now();
    }

    public void reject(User admin, String reason) {
        this.status = VendorStatus.REJECTED;
        this.rejectionReason = reason.trim();
        this.reviewedBy = admin;
        this.reviewedAt = Instant.now();
    }

    private void applyDetails(String storeName, String storeDescription, String contactPhone,
                              String businessAddress, String gstNumber) {
        this.storeName = storeName.trim();
        this.storeDescription = blankToNull(storeDescription);
        this.contactPhone = contactPhone.trim();
        this.businessAddress = businessAddress.trim();
        String gst = blankToNull(gstNumber);
        this.gstNumber = gst == null ? null : gst.toUpperCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
