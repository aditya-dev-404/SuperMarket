package com.aditya.supermarket.address.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A saved delivery address of a user. At checkout the address is copied into the order, so editing
 * or deleting it later never changes an existing order.
 */
@Getter
@Entity
@Table(name = "addresses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, length = 200)
    private String line1;

    @Column(length = 200)
    private String line2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    public Address(Long userId, String fullName, String phone, String line1, String line2,
                   String city, String state, String pincode) {
        this.userId = userId;
        applyDetails(fullName, phone, line1, line2, city, state, pincode);
    }

    public void updateDetails(String fullName, String phone, String line1, String line2,
                              String city, String state, String pincode) {
        applyDetails(fullName, phone, line1, line2, city, state, pincode);
    }

    public void markAsDefault() {
        this.defaultAddress = true;
    }

    public void clearDefault() {
        this.defaultAddress = false;
    }

    private void applyDetails(String fullName, String phone, String line1, String line2,
                              String city, String state, String pincode) {
        this.fullName = fullName.trim();
        this.phone = phone.trim();
        this.line1 = line1.trim();
        this.line2 = blankToNull(line2);
        this.city = city.trim();
        this.state = state.trim();
        this.pincode = pincode.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
