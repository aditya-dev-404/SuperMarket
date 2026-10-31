package com.aditya.supermarket.address.service;

import com.aditya.supermarket.address.config.AddressProperties;
import com.aditya.supermarket.address.dto.AddressRequest;
import com.aditya.supermarket.address.dto.AddressResponse;
import com.aditya.supermarket.address.entity.Address;
import com.aditya.supermarket.address.repository.AddressRepository;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A user's saved delivery addresses. A user always has at most one default address; the default only
 * moves when the user picks another one (or deletes the default, then the newest remaining takes over).
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final AddressProperties addressProperties;

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addressRepository.findAllByUserIdOrderByDefaultAddressDescIdDesc(userId).stream()
            .map(AddressResponse::from)
            .toList();
    }

    /** Used by checkout: returns the address only if it belongs to the given user. */
    @Transactional(readOnly = true)
    public Address getOwned(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        long existing = addressRepository.countByUserId(userId);
        int max = addressProperties.maxPerUser();
        if (existing >= max) {
            throw new BusinessRuleException("You can save at most " + max + " addresses");
        }

        Address address = new Address(userId, request.fullName(), request.phone(), request.line1(),
            request.line2(), request.city(), request.state(), request.pincode());
        // The first address becomes the default so checkout always has one to preselect.
        if (existing == 0 || request.defaultAddress()) {
            clearDefaults(userId);
            address.markAsDefault();
        }
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        Address address = getOwned(userId, addressId);
        address.updateDetails(request.fullName(), request.phone(), request.line1(), request.line2(),
            request.city(), request.state(), request.pincode());
        // Never demotes: a user must not end up without a default by editing; they pick another one instead.
        if (request.defaultAddress() && !address.isDefaultAddress()) {
            clearDefaults(userId);
            address.markAsDefault();
        }
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse setDefault(Long userId, Long addressId) {
        Address address = getOwned(userId, addressId);
        if (!address.isDefaultAddress()) {
            clearDefaults(userId);
            address.markAsDefault();
        }
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = getOwned(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        if (wasDefault) {
            addressRepository.findFirstByUserIdAndIdNotOrderByIdDesc(userId, addressId)
                .ifPresent(Address::markAsDefault);
        }
    }

    private void clearDefaults(Long userId) {
        addressRepository.findAllByUserIdAndDefaultAddressTrue(userId).forEach(Address::clearDefault);
    }
}
