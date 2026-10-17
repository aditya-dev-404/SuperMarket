package com.aditya.supermarket.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.auth.config.JwtProperties;
import com.aditya.supermarket.auth.entity.RefreshToken;
import com.aditya.supermarket.auth.repository.RefreshTokenRepository;
import com.aditya.supermarket.auth.service.RefreshTokenService.IssuedToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "supermarket", "x".repeat(32), Duration.ofMinutes(15), Duration.ofDays(7));
        service = new RefreshTokenService(repository, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issueStoresOnlyTheHashOfTheToken() {
        IssuedToken issued = service.issue(7L);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash())
                .isEqualTo(RefreshTokenService.hash(issued.value()))
                .isNotEqualTo(issued.value())
                .hasSize(64);
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void consumeRevokesTheTokenAndReturnsTheUser() {
        RefreshToken token = new RefreshToken(RefreshTokenService.hash("raw"), 7L, NOW.plusSeconds(60));
        when(repository.findByTokenHash(RefreshTokenService.hash("raw"))).thenReturn(Optional.of(token));

        Long userId = service.consume("raw");

        assertThat(userId).isEqualTo(7L);
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void reusingARevokedTokenRevokesEverySessionOfTheUser() {
        RefreshToken token = new RefreshToken(RefreshTokenService.hash("raw"), 7L, NOW.plusSeconds(60));
        token.revoke();
        when(repository.findByTokenHash(RefreshTokenService.hash("raw"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume("raw")).isInstanceOf(BadCredentialsException.class);

        verify(repository).revokeAllForUser(7L);
    }

    @Test
    void expiredTokenIsRejected() {
        RefreshToken token = new RefreshToken(RefreshTokenService.hash("raw"), 7L, NOW.minusSeconds(1));
        when(repository.findByTokenHash(RefreshTokenService.hash("raw"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume("raw")).isInstanceOf(BadCredentialsException.class);

        verify(repository, never()).revokeAllForUser(any());
    }

    @Test
    void unknownTokenIsRejected() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("nope")).isInstanceOf(BadCredentialsException.class);
    }
}
