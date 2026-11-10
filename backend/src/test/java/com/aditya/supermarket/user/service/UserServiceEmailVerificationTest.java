package com.aditya.supermarket.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Covers the Phase 7 additions to {@link UserService}. */
@ExtendWith(MockitoExtension.class)
class UserServiceEmailVerificationTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void newAccountsStartUnverified() {
        User user = new User("a@example.com", "hash", "Aditya", "Kumar", Role.CUSTOMER);

        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getEmailVerifiedAt()).isNull();
    }

    @Test
    void markEmailVerified_keepsTheFirstConfirmationTime() {
        User user = existing();
        user.markEmailVerified(NOW);

        user.markEmailVerified(NOW.plusSeconds(3600));

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void resetPassword_setsTheNewHashAndVerifiesTheAddress() {
        User user = existing();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassw0rd")).thenReturn("new-hash");
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        service.resetPassword(7L, "NewPassw0rd", NOW);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.isEmailVerified()).isTrue();
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void createAdminIfAbsent_createsAVerifiedAdminSoTheOperatorCanLogIn() {
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd1")).thenReturn("hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

        boolean created = service.createAdminIfAbsent("admin@example.com", "Passw0rd1");

        assertThat(created).isTrue();
        org.mockito.ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().isEmailVerified()).isTrue();
    }

    @Test
    void deleteUnverifiedCreatedBefore_delegatesToTheRepository() {
        when(userRepository.deleteUnverifiedCreatedBefore(NOW)).thenReturn(3);

        assertThat(service.deleteUnverifiedCreatedBefore(NOW)).isEqualTo(3);
    }

    private static User existing() {
        User user = new User("a@example.com", "hash", "Aditya", "Kumar", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    @Test
    void findAdminEmails_asksForDeliverableAdminAccounts() {
        when(userRepository.findDeliverableEmailsByRole(Role.ADMIN))
            .thenReturn(java.util.List.of("admin@example.com"));

        assertThat(service.findAdminEmails()).containsExactly("admin@example.com");
    }
}
