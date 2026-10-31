package com.aditya.supermarket.address.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.address.config.AddressProperties;
import com.aditya.supermarket.address.dto.AddressRequest;
import com.aditya.supermarket.address.dto.AddressResponse;
import com.aditya.supermarket.address.entity.Address;
import com.aditya.supermarket.address.repository.AddressRepository;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    private static final long USER_ID = 1L;
    private static final long ADDRESS_ID = 10L;
    private static final long OTHER_ADDRESS_ID = 11L;
    private static final int MAX_ADDRESSES = 2;

    @Mock
    private AddressRepository addressRepository;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressRepository, new AddressProperties(MAX_ADDRESSES));
    }

    private static AddressRequest request(boolean defaultAddress) {
        return new AddressRequest("Aditya Kumar", "9876543210", "Flat 12, Main Road", null,
            "Greater Noida", "Uttar Pradesh", "201310", defaultAddress);
    }

    private static Address address(long id, boolean defaultAddress) {
        Address address = new Address(USER_ID, "Someone", "9876543210", "Line 1", null, "City", "State", "201310");
        ReflectionTestUtils.setField(address, "id", id);
        if (defaultAddress) {
            address.markAsDefault();
        }
        return address;
    }

    private void givenSaveReturnsItsArgument() {
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenOwnedAddress(Address address) {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
    }

    @Test
    void create_makesTheFirstAddressDefault() {
        when(addressRepository.countByUserId(USER_ID)).thenReturn(0L);
        givenSaveReturnsItsArgument();

        AddressResponse response = addressService.create(USER_ID, request(false));

        assertThat(response.defaultAddress()).isTrue();
    }

    @Test
    void create_keepsTheExistingDefault_whenNotRequested() {
        when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
        givenSaveReturnsItsArgument();

        AddressResponse response = addressService.create(USER_ID, request(false));

        assertThat(response.defaultAddress()).isFalse();
        verify(addressRepository, never()).findAllByUserIdAndDefaultAddressTrue(USER_ID);
    }

    @Test
    void create_movesTheDefault_whenRequested() {
        Address currentDefault = address(5, true);
        when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
        when(addressRepository.findAllByUserIdAndDefaultAddressTrue(USER_ID)).thenReturn(List.of(currentDefault));
        givenSaveReturnsItsArgument();

        AddressResponse response = addressService.create(USER_ID, request(true));

        assertThat(response.defaultAddress()).isTrue();
        assertThat(currentDefault.isDefaultAddress()).isFalse();
    }

    @Test
    void create_rejects_whenTheLimitIsReached() {
        when(addressRepository.countByUserId(USER_ID)).thenReturn((long) MAX_ADDRESSES);

        assertThatThrownBy(() -> addressService.create(USER_ID, request(false)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("at most 2");
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void update_changesTheDetails() {
        givenOwnedAddress(address(ADDRESS_ID, false));

        AddressResponse response = addressService.update(USER_ID, ADDRESS_ID, request(false));

        assertThat(response.fullName()).isEqualTo("Aditya Kumar");
        assertThat(response.city()).isEqualTo("Greater Noida");
        assertThat(response.pincode()).isEqualTo("201310");
        assertThat(response.defaultAddress()).isFalse();
    }

    @Test
    void update_throwsNotFound_forSomeoneElsesOrMissingAddress() {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.update(USER_ID, ADDRESS_ID, request(false)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_neverRemovesTheDefault() {
        givenOwnedAddress(address(ADDRESS_ID, true));

        AddressResponse response = addressService.update(USER_ID, ADDRESS_ID, request(false));

        assertThat(response.defaultAddress()).isTrue();
    }

    @Test
    void update_movesTheDefault_whenRequested() {
        Address edited = address(ADDRESS_ID, false);
        Address currentDefault = address(OTHER_ADDRESS_ID, true);
        givenOwnedAddress(edited);
        when(addressRepository.findAllByUserIdAndDefaultAddressTrue(USER_ID)).thenReturn(List.of(currentDefault));

        addressService.update(USER_ID, ADDRESS_ID, request(true));

        assertThat(edited.isDefaultAddress()).isTrue();
        assertThat(currentDefault.isDefaultAddress()).isFalse();
    }

    @Test
    void setDefault_movesTheDefault() {
        Address chosen = address(ADDRESS_ID, false);
        Address currentDefault = address(OTHER_ADDRESS_ID, true);
        givenOwnedAddress(chosen);
        when(addressRepository.findAllByUserIdAndDefaultAddressTrue(USER_ID)).thenReturn(List.of(currentDefault));

        AddressResponse response = addressService.setDefault(USER_ID, ADDRESS_ID);

        assertThat(response.defaultAddress()).isTrue();
        assertThat(currentDefault.isDefaultAddress()).isFalse();
    }

    @Test
    void setDefault_doesNothing_whenAlreadyDefault() {
        givenOwnedAddress(address(ADDRESS_ID, true));

        AddressResponse response = addressService.setDefault(USER_ID, ADDRESS_ID);

        assertThat(response.defaultAddress()).isTrue();
        verify(addressRepository, never()).findAllByUserIdAndDefaultAddressTrue(USER_ID);
    }

    @Test
    void delete_promotesTheNewestOtherAddress_whenTheDefaultIsDeleted() {
        Address deleted = address(ADDRESS_ID, true);
        Address other = address(OTHER_ADDRESS_ID, false);
        givenOwnedAddress(deleted);
        when(addressRepository.findFirstByUserIdAndIdNotOrderByIdDesc(USER_ID, ADDRESS_ID))
            .thenReturn(Optional.of(other));

        addressService.delete(USER_ID, ADDRESS_ID);

        verify(addressRepository).delete(deleted);
        assertThat(other.isDefaultAddress()).isTrue();
    }

    @Test
    void delete_keepsTheDefault_whenAnotherAddressIsDeleted() {
        Address deleted = address(ADDRESS_ID, false);
        givenOwnedAddress(deleted);

        addressService.delete(USER_ID, ADDRESS_ID);

        verify(addressRepository).delete(deleted);
        verify(addressRepository, never()).findFirstByUserIdAndIdNotOrderByIdDesc(USER_ID, ADDRESS_ID);
    }

    @Test
    void delete_throwsNotFound_forSomeoneElsesOrMissingAddress() {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.delete(USER_ID, ADDRESS_ID))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(addressRepository, never()).delete(any(Address.class));
    }

    @Test
    void list_returnsTheUsersAddresses() {
        when(addressRepository.findAllByUserIdOrderByDefaultAddressDescIdDesc(USER_ID))
            .thenReturn(List.of(address(ADDRESS_ID, true), address(OTHER_ADDRESS_ID, false)));

        List<AddressResponse> addresses = addressService.list(USER_ID);

        assertThat(addresses).extracting(AddressResponse::id).containsExactly(ADDRESS_ID, OTHER_ADDRESS_ID);
        assertThat(addresses.getFirst().defaultAddress()).isTrue();
    }

    @Test
    void getOwned_returnsTheAddress_forItsOwner() {
        Address owned = address(ADDRESS_ID, false);
        givenOwnedAddress(owned);

        assertThat(addressService.getOwned(USER_ID, ADDRESS_ID)).isSameAs(owned);
    }

    @Test
    void getOwned_throwsNotFound_forSomeoneElsesAddress() {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getOwned(USER_ID, ADDRESS_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
