package com.aditya.supermarket.order.dto;

import com.aditya.supermarket.order.entity.ShippingAddress;

public record ShippingAddressResponse(
    String fullName,
    String phone,
    String line1,
    String line2,
    String city,
    String state,
    String pincode) {

    public static ShippingAddressResponse from(ShippingAddress address) {
        return new ShippingAddressResponse(
            address.getFullName(),
            address.getPhone(),
            address.getLine1(),
            address.getLine2(),
            address.getCity(),
            address.getState(),
            address.getPincode());
    }
}
