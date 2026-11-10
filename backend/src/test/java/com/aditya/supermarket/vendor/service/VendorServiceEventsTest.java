package com.aditya.supermarket.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.notification.event.VendorApplicationApprovedEvent;
import com.aditya.supermarket.notification.event.VendorApplicationRejectedEvent;
import com.aditya.supermarket.notification.event.VendorApplicationSubmittedEvent;
import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.service.UserService;
import com.aditya.supermarket.vendor.dto.VendorApplicationRequest;
import com.aditya.supermarket.vendor.entity.Vendor;
import com.aditya.supermarket.vendor.entity.VendorStatus;
import com.aditya.supermarket.vendor.repository.VendorRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/** Checks that VendorService announces onboarding changes so the notification module can send emails. */
@ExtendWith(MockitoExtension.class)
class VendorServiceEventsTest {

    private static final long USER_ID = 42L;
    private static final long VENDOR_ID = 7L;
    private static final long ADMIN_ID = 1L;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher events;

    private VendorService service;
    private User applicant;

    @BeforeEach
    void setUp() {
        service = new VendorService(vendorRepository, userService, events);
        applicant = user(USER_ID, "aditya@example.com", "Aditya", "Kumar", Role.CUSTOMER);
    }

    @Test
    void apply_publishesASubmittedEvent() {
        when(userService.getById(USER_ID)).thenReturn(applicant);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(call -> {
            Vendor saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", VENDOR_ID);
            return saved;
        });

        service.apply(USER_ID, request("Fresh Mart"));

        VendorApplicationSubmittedEvent event = captured(VendorApplicationSubmittedEvent.class);
        assertThat(event.vendorId()).isEqualTo(VENDOR_ID);
        assertThat(event.applicantEmail()).isEqualTo("aditya@example.com");
        assertThat(event.applicantFullName()).isEqualTo("Aditya Kumar");
        assertThat(event.storeName()).isEqualTo("Fresh Mart");
        assertThat(event.resubmission()).isFalse();
    }

    @Test
    void apply_whenAlreadyApplied_publishesNothing() {
        when(userService.getById(USER_ID)).thenReturn(applicant);
        when(vendorRepository.existsByUserId(USER_ID)).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.apply(USER_ID, request("Fresh Mart")))
            .isInstanceOf(RuntimeException.class);

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void updateMine_resubmittingARejectedApplicationPublishesAResubmissionEvent() {
        Vendor vendor = vendorWithStatus(VendorStatus.REJECTED);
        when(vendorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(vendor));

        service.updateMine(USER_ID, request("Fresh Mart 2"));

        VendorApplicationSubmittedEvent event = captured(VendorApplicationSubmittedEvent.class);
        assertThat(event.resubmission()).isTrue();
        assertThat(event.storeName()).isEqualTo("Fresh Mart 2");
        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.PENDING);
    }

    @Test
    void updateMine_editingAPendingApplicationPublishesNothing() {
        Vendor vendor = vendorWithStatus(VendorStatus.PENDING);
        when(vendorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(vendor));

        service.updateMine(USER_ID, request("Fresh Mart 2"));

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void approve_publishesAnApprovedEvent() {
        Vendor vendor = vendorWithStatus(VendorStatus.PENDING);
        when(vendorRepository.findById(VENDOR_ID)).thenReturn(Optional.of(vendor));
        when(userService.getById(ADMIN_ID)).thenReturn(user(ADMIN_ID, "admin@example.com", "Platform", "Admin", Role.ADMIN));

        service.approve(VENDOR_ID, ADMIN_ID);

        VendorApplicationApprovedEvent event = captured(VendorApplicationApprovedEvent.class);
        assertThat(event.applicantEmail()).isEqualTo("aditya@example.com");
        assertThat(event.storeName()).isEqualTo("Fresh Mart");
        assertThat(event.vendorId()).isEqualTo(VENDOR_ID);
    }

    @Test
    void reject_publishesARejectedEventWithTheReason() {
        Vendor vendor = vendorWithStatus(VendorStatus.PENDING);
        when(vendorRepository.findById(VENDOR_ID)).thenReturn(Optional.of(vendor));
        when(userService.getById(ADMIN_ID)).thenReturn(user(ADMIN_ID, "admin@example.com", "Platform", "Admin", Role.ADMIN));

        service.reject(VENDOR_ID, "  GST number could not be verified  ", ADMIN_ID);

        VendorApplicationRejectedEvent event = captured(VendorApplicationRejectedEvent.class);
        assertThat(event.reason()).isEqualTo("GST number could not be verified");
        assertThat(event.applicantEmail()).isEqualTo("aditya@example.com");
    }

    @Test
    void approve_whenNotPending_publishesNothing() {
        Vendor vendor = vendorWithStatus(VendorStatus.APPROVED);
        when(vendorRepository.findById(VENDOR_ID)).thenReturn(Optional.of(vendor));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.approve(VENDOR_ID, ADMIN_ID))
            .isInstanceOf(RuntimeException.class);

        verify(events, never()).publishEvent(any(Object.class));
    }

    private Vendor vendorWithStatus(VendorStatus status) {
        Vendor vendor = new Vendor(applicant, "Fresh Mart", null, "9876543210", "12 Market Road, Noida", null);
        ReflectionTestUtils.setField(vendor, "id", VENDOR_ID);
        ReflectionTestUtils.setField(vendor, "status", status);
        return vendor;
    }

    private static User user(long id, String email, String first, String last, Role role) {
        User user = new User(email, "hash", first, last, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static VendorApplicationRequest request(String storeName) {
        return new VendorApplicationRequest(storeName, null, "9876543210", "12 Market Road, Noida", null);
    }

    private <T> T captured(Class<T> type) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(type);
        return type.cast(captor.getValue());
    }
}
