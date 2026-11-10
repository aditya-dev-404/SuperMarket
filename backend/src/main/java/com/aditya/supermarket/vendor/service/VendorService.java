package com.aditya.supermarket.vendor.service;

import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.DuplicateResourceException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.notification.event.VendorApplicationApprovedEvent;
import com.aditya.supermarket.notification.event.VendorApplicationRejectedEvent;
import com.aditya.supermarket.notification.event.VendorApplicationSubmittedEvent;
import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.service.UserService;
import com.aditya.supermarket.vendor.dto.VendorApplicationRequest;
import com.aditya.supermarket.vendor.dto.VendorApplicationResponse;
import com.aditya.supermarket.vendor.entity.Vendor;
import com.aditya.supermarket.vendor.entity.VendorStatus;
import com.aditya.supermarket.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vendor onboarding: customers apply, admins approve or reject. */
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    // ---------- applicant ----------

    @Transactional
    public VendorApplicationResponse apply(Long userId, VendorApplicationRequest request) {
        User user = userService.getById(userId);
        if (user.getRole() != Role.CUSTOMER) {
            throw new BusinessRuleException("Only customer accounts can apply to become a vendor");
        }
        if (vendorRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("You have already submitted a vendor application");
        }
        ensureStoreNameAvailable(request.storeName(), null);
        Vendor vendor = vendorRepository.save(new Vendor(
            user,
            request.storeName(),
            request.storeDescription(),
            request.contactPhone(),
            request.businessAddress(),
            request.gstNumber()));
        events.publishEvent(submittedEvent(vendor, false));
        return VendorApplicationResponse.from(vendor);
    }

    @Transactional(readOnly = true)
    public VendorApplicationResponse getMine(Long userId) {
        return VendorApplicationResponse.from(findByUserId(userId));
    }

    @Transactional
    public VendorApplicationResponse updateMine(Long userId, VendorApplicationRequest request) {
        Vendor vendor = findByUserId(userId);
        if (vendor.getStatus() == VendorStatus.APPROVED) {
            throw new BusinessRuleException("An approved application can no longer be edited");
        }
        ensureStoreNameAvailable(request.storeName(), vendor.getId());
        boolean resubmission = vendor.getStatus() == VendorStatus.REJECTED;
        vendor.updateDetails(
            request.storeName(),
            request.storeDescription(),
            request.contactPhone(),
            request.businessAddress(),
            request.gstNumber());
        if (resubmission) {
            // Only a rejected application going back to review is news; plain edits of a pending one are not.
            events.publishEvent(submittedEvent(vendor, true));
        }
        return VendorApplicationResponse.from(vendor);
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public PagedModel<VendorApplicationResponse> list(VendorStatus status, Pageable pageable) {
        Page<Vendor> page = status == null
            ? vendorRepository.findAll(pageable)
            : vendorRepository.findByStatus(status, pageable);
        return new PagedModel<>(page.map(VendorApplicationResponse::from));
    }

    @Transactional(readOnly = true)
    public VendorApplicationResponse get(Long id) {
        return VendorApplicationResponse.from(findById(id));
    }

    @Transactional
    public VendorApplicationResponse approve(Long id, Long adminId) {
        Vendor vendor = findById(id);
        ensurePending(vendor);
        vendor.approve(userService.getById(adminId));
        vendor.getUser().promoteToVendor();
        User applicant = vendor.getUser();
        events.publishEvent(new VendorApplicationApprovedEvent(
            vendor.getId(), applicant.getId(), applicant.getEmail(), applicant.getFirstName(), vendor.getStoreName()));
        return VendorApplicationResponse.from(vendor);
    }

    @Transactional
    public VendorApplicationResponse reject(Long id, String reason, Long adminId) {
        Vendor vendor = findById(id);
        ensurePending(vendor);
        vendor.reject(userService.getById(adminId), reason);
        User applicant = vendor.getUser();
        events.publishEvent(new VendorApplicationRejectedEvent(
            vendor.getId(), applicant.getId(), applicant.getEmail(), applicant.getFirstName(),
            vendor.getStoreName(), vendor.getRejectionReason()));
        return VendorApplicationResponse.from(vendor);
    }

    // ---------- helpers ----------

    private static VendorApplicationSubmittedEvent submittedEvent(Vendor vendor, boolean resubmission) {
        User applicant = vendor.getUser();
        return new VendorApplicationSubmittedEvent(
            vendor.getId(), applicant.getId(), applicant.getEmail(), applicant.getFirstName(),
            applicant.getFirstName() + " " + applicant.getLastName(),
            vendor.getStoreName(), vendor.getContactPhone(), vendor.getGstNumber(), resubmission);
    }

    private Vendor findById(Long id) {
        return vendorRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Vendor application", id));
    }

    private Vendor findByUserId(Long userId) {
        return vendorRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("You have not applied to become a vendor yet"));
    }

    private void ensurePending(Vendor vendor) {
        if (vendor.getStatus() != VendorStatus.PENDING) {
            throw new BusinessRuleException(
                "Only pending applications can be reviewed (current status: %s)".formatted(vendor.getStatus()));
        }
    }

    private void ensureStoreNameAvailable(String storeName, Long excludeVendorId) {
        vendorRepository.findByStoreNameIgnoreCase(storeName.trim())
            .filter(existing -> !existing.getId().equals(excludeVendorId))
            .ifPresent(existing -> {
                throw new DuplicateResourceException("This store name is already taken");
            });
    }
}
