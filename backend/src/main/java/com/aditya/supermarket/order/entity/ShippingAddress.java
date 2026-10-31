package com.aditya.supermarket.order.entity;

import com.aditya.supermarket.address.entity.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Copy of the delivery address taken at checkout, so later address edits never change an order. */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress {

    @Column(name = "shipping_full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "shipping_phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "shipping_line1", nullable = false, length = 200)
    private String line1;

    @Column(name = "shipping_line2", length = 200)
    private String line2;

    @Column(name = "shipping_city", nullable = false, length = 100)
    private String city;

    @Column(name = "shipping_state", nullable = false, length = 100)
    private String state;

    @Column(name = "shipping_pincode", nullable = false, length = 6)
    private String pincode;

    public static ShippingAddress from(Address address) {
        ShippingAddress copy = new ShippingAddress();
        copy.fullName = address.getFullName();
        copy.phone = address.getPhone();
        copy.line1 = address.getLine1();
        copy.line2 = address.getLine2();
        copy.city = address.getCity();
        copy.state = address.getState();
        copy.pincode = address.getPincode();
        return copy;
    }
}
