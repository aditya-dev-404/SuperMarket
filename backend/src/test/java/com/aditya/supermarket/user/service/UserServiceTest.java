package com.aditya.supermarket.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.DuplicateResourceException;
import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsCustomerRole() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd1")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.registerCustomer("  John@Example.COM ", "Passw0rd1", " John ", "Doe ");

        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void registerWithExistingEmailFails() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerCustomer("john@example.com", "Passw0rd1", "John", "Doe"))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void changePasswordWithWrongCurrentPasswordFails() {
        User user = new User("john@example.com", "old-hash", "John", "Doe", Role.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrong", "NewPassw0rd"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void changePasswordStoresTheNewHash() {
        User user = new User("john@example.com", "old-hash", "John", "Doe", Role.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassw0rd", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassw0rd")).thenReturn("new-hash");

        userService.changePassword(1L, "OldPassw0rd", "NewPassw0rd");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }
}
