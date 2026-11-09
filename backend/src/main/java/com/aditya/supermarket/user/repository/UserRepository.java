package com.aditya.supermarket.user.repository;

import com.aditya.supermarket.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Deletes customer accounts that never confirmed their email and were created before the cutoff.
     * Their one-time tokens are removed by the database (ON DELETE CASCADE). Returns the number deleted.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from User u
            where u.emailVerified = false
              and u.role = com.aditya.supermarket.user.entity.Role.CUSTOMER
              and u.createdAt < :cutoff
            """)
    int deleteUnverifiedCreatedBefore(@Param("cutoff") Instant cutoff);
}
