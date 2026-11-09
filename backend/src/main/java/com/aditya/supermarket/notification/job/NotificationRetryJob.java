package com.aditya.supermarket.notification.job;

import com.aditya.supermarket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically retries emails that failed earlier and whose back-off time has passed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryJob {

    private final NotificationService notificationService;

    @Scheduled(
        fixedDelayString = "${app.notification.retry-interval:PT1M}",
        initialDelayString = "PT30S")
    public void retryPending() {
        int handled = notificationService.retryDue();
        if (handled > 0) {
            log.info("Notification retry job handled {} email(s)", handled);
        }
    }
}
