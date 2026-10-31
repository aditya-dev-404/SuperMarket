package com.aditya.supermarket.address.dto;

import com.aditya.supermarket.address.entity.Address;

public record AddressResponse(
    Long id,
    String fullName,
    String phone,
    String line1,
    String line2,
    String city,
    String state,
    String pincode,
    boolean defaultAddress) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
            address.getId(),
            address.getFullName(),
            address.getPhone(),
            address.getLine1(),
            address.getLine2(),
            address.getCity(),
            address.getState(),
            address.getPincode(),
            address.isDefaultAddress());
    }
}
