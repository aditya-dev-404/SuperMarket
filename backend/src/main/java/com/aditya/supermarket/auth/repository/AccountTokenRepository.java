package com.aditya.supermarket.auth.repository;

import com.aditya.supermarket.auth.entity.AccountToken;
import com.aditya.supermarket.auth.entity.AccountTokenType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

    Optional<AccountToken> findByTokenHash(String tokenHash);

    Optional<AccountToken> findFirstByUserIdAndTypeOrderByCreatedAtDesc(Long userId, AccountTokenType type);

    /**
     * Burns every still-unused token of this kind for the user, so only the newest link works.
     * Returns the number of tokens burned.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AccountToken t set t.usedAt = :now
            where t.userId = :userId and t.type = :type and t.usedAt is null
            """)
    int invalidateUnused(@Param("userId") Long userId,
                         @Param("type") AccountTokenType type,
                         @Param("now") Instant now);

    /**
     * Atomically marks a token as used if (and only if) it exists, matches the type, is unused and
     * not expired. Returns 1 on success and 0 otherwise, which makes a token strictly single-use
     * even when two requests arrive at the same time.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AccountToken t set t.usedAt = :now
            where t.tokenHash = :tokenHash and t.type = :type
              and t.usedAt is null and t.expiresAt > :now
            """)
    int markUsed(@Param("tokenHash") String tokenHash,
                 @Param("type") AccountTokenType type,
                 @Param("now") Instant now);
}
