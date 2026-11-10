package com.aditya.supermarket.user.service;

import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.DuplicateResourceException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.user.dto.UpdateProfileRequest;
import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User accounts: creation, lookup, profile and password management. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String DUPLICATE_EMAIL_MESSAGE = "An account with this email already exists";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerCustomer(String email, String rawPassword, String firstName, String lastName) {
        return create(email, rawPassword, firstName, lastName, Role.CUSTOMER);
    }

    /** Creates the admin account unless one with that email exists. Returns true if created. */
    @Transactional
    public boolean createAdminIfAbsent(String email, String rawPassword) {
        if (userRepository.existsByEmail(normalize(email))) {
            return false;
        }
        User admin = create(email, rawPassword, "Platform", "Admin", Role.ADMIN);
        // The admin is configured by the operator, so there is no mailbox to confirm.
        admin.markEmailVerified(Instant.now());
        return true;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalize(email));
    }

    /** Account emails of every active admin; used for admin alerts. */
    public List<String> findAdminEmails() {
        return userRepository.findDeliverableEmailsByRole(Role.ADMIN);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    @Transactional
    public User updateProfile(Long id, UpdateProfileRequest request) {
        User user = getById(id);
        user.updateProfile(request.firstName().trim(), request.lastName().trim(), blankToNull(request.phone()));
        return user;
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        User user = getById(id);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            // 422, not 401: a 401 would make the frontend think the session expired.
            throw new BusinessRuleException("Current password is incorrect");
        }
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        // Write now: AuthService revokes refresh tokens right after this with a bulk query that
        // clears the persistence context, which would silently discard an unflushed change.
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public User markEmailVerified(Long id, Instant now) {
        User user = getById(id);
        user.markEmailVerified(now);
        return user;
    }

    /**
     * Sets a new password from a password-reset link. The link was delivered to the account's own
     * mailbox, so using it also proves the address belongs to the user and verifies it.
     */
    @Transactional
    public User resetPassword(Long id, String newPassword, Instant now) {
        User user = getById(id);
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        user.markEmailVerified(now);
        // Write now: the caller revokes refresh tokens next with a bulk query that clears the context.
        return userRepository.saveAndFlush(user);
    }

    /** Housekeeping: removes unverified customer accounts created before the cutoff. */
    @Transactional
    public int deleteUnverifiedCreatedBefore(Instant cutoff) {
        return userRepository.deleteUnverifiedCreatedBefore(cutoff);
    }

    private User create(String email, String rawPassword, String firstName, String lastName, Role role) {
        String normalizedEmail = normalize(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(DUPLICATE_EMAIL_MESSAGE);
        }
        User user = new User(
            normalizedEmail,
            passwordEncoder.encode(rawPassword),
            firstName.trim(),
            lastName.trim(),
            role);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent registrations with the same email: the unique index wins.
            throw new DuplicateResourceException(DUPLICATE_EMAIL_MESSAGE);
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
