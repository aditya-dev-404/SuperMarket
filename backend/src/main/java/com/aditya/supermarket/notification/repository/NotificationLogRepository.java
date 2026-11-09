package com.aditya.supermarket.notification.repository;

import com.aditya.supermarket.notification.entity.NotificationLog;
import com.aditya.supermarket.notification.entity.NotificationStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /** Emails that are still waiting for delivery and whose retry time has come, oldest first. */
    List<NotificationLog> findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
        NotificationStatus status, Instant dueBefore, Pageable pageable);
}
