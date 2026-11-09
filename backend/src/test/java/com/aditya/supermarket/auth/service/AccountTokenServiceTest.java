package com.aditya.supermarket.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.auth.config.AccountProperties;
import com.aditya.supermarket.auth.entity.AccountToken;
import com.aditya.supermarket.auth.entity.AccountTokenType;
import com.aditya.supermarket.auth.repository.AccountTokenRepository;
import com.aditya.supermarket.common.exception.BusinessRuleException;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final long USER_ID = 42L;

    @Mock
    private AccountTokenRepository repository;

    private AccountTokenService service;

    @BeforeEach
    void setUp() {
        AccountProperties properties = new AccountProperties(
            Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofMinutes(1), Duration.ofDays(7));
        service = new AccountTokenService(repository, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issue_storesOnlyTheHashOfTheTokenAndBurnsOlderOnes() {
        IssuedAccountToken issued = service.issue(USER_ID, AccountTokenType.EMAIL_VERIFICATION);

        ArgumentCaptor<AccountToken> saved = ArgumentCaptor.forClass(AccountToken.class);
        verify(repository).invalidateUnused(USER_ID, AccountTokenType.EMAIL_VERIFICATION, NOW);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash())
            .isEqualTo(AccountTokenService.hash(issued.rawToken()))
            .isNotEqualTo(issued.rawToken())
            .hasSize(64);
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(issued.rawToken()).hasSizeGreaterThanOrEqualTo(40);
    }

    @Test
    void issue_usesTheValidityOfEachTokenType() {
        IssuedAccountToken verification = service.issue(USER_ID, AccountTokenType.EMAIL_VERIFICATION);
        IssuedAccountToken reset = service.issue(USER_ID, AccountTokenType.PASSWORD_RESET);

        assertThat(verification.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(reset.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(reset.validFor()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void issue_createsDifferentTokensEachTime() {
        String first = service.issue(USER_ID, AccountTokenType.PASSWORD_RESET).rawToken();
        String second = service.issue(USER_ID, AccountTokenType.PASSWORD_RESET).rawToken();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void issueUnlessRecentlyIssued_skipsDuringCooldown() {
        AccountToken latest = tokenCreatedAt(NOW.minusSeconds(30));
        when(repository.findFirstByUserIdAndTypeOrderByCreatedAtDesc(USER_ID, AccountTokenType.PASSWORD_RESET))
            .thenReturn(Optional.of(latest));

        Optional<IssuedAccountToken> result = service.issueUnlessRecentlyIssued(USER_ID, AccountTokenType.PASSWORD_RESET);

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void issueUnlessRecentlyIssued_issuesAfterCooldown() {
        AccountToken latest = tokenCreatedAt(NOW.minusSeconds(61));
        when(repository.findFirstByUserIdAndTypeOrderByCreatedAtDesc(USER_ID, AccountTokenType.PASSWORD_RESET))
            .thenReturn(Optional.of(latest));

        assertThat(service.issueUnlessRecentlyIssued(USER_ID, AccountTokenType.PASSWORD_RESET)).isPresent();
    }

    @Test
    void issueUnlessRecentlyIssued_issuesWhenNothingWasIssuedBefore() {
        when(repository.findFirstByUserIdAndTypeOrderByCreatedAtDesc(USER_ID, AccountTokenType.EMAIL_VERIFICATION))
            .thenReturn(Optional.empty());

        assertThat(service.issueUnlessRecentlyIssued(USER_ID, AccountTokenType.EMAIL_VERIFICATION)).isPresent();
    }

    @Test
    void redeem_returnsTheUserIdOfAValidToken() {
        String raw = "raw-token";
        String hash = AccountTokenService.hash(raw);
        when(repository.markUsed(hash, AccountTokenType.EMAIL_VERIFICATION, NOW)).thenReturn(1);
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(tokenCreatedAt(NOW)));

        assertThat(service.redeem(raw, AccountTokenType.EMAIL_VERIFICATION)).isEqualTo(USER_ID);
    }

    @Test
    void redeem_rejectsUnknownExpiredOrUsedTokensWithOneMessage() {
        when(repository.markUsed(any(), eq(AccountTokenType.PASSWORD_RESET), eq(NOW))).thenReturn(0);

        assertThatThrownBy(() -> service.redeem("whatever", AccountTokenType.PASSWORD_RESET))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage(AccountTokenService.INVALID_TOKEN_MESSAGE);
    }

    @Test
    void redeem_rejectsBlankTokensWithoutTouchingTheDatabase() {
        assertThatThrownBy(() -> service.redeem("  ", AccountTokenType.PASSWORD_RESET))
            .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.redeem(null, AccountTokenType.PASSWORD_RESET))
            .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).markUsed(any(), any(), any());
    }

    @Test
    void hash_isStableAndSixtyFourHexCharacters() {
        assertThat(AccountTokenService.hash("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void issuedToken_toStringDoesNotRevealTheToken() {
        IssuedAccountToken issued = new IssuedAccountToken("super-secret", NOW, Duration.ofMinutes(5));

        assertThat(issued.toString()).doesNotContain("super-secret");
    }

    private static AccountToken tokenCreatedAt(Instant createdAt) {
        AccountToken token = AccountToken.issue(USER_ID, AccountTokenType.PASSWORD_RESET, "hash", NOW.plusSeconds(60));
        ReflectionTestUtils.setField(token, "createdAt", createdAt);
        return token;
    }
}
