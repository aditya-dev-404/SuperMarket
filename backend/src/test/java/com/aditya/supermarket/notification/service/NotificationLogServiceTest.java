package com.aditya.supermarket.notification.service;

import static com.aditya.supermarket.notification.NotificationTestData.NOW;
import static com.aditya.supermarket.notification.NotificationTestData.pendingLog;
import static com.aditya.supermarket.notification.NotificationTestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.notification.entity.NotificationLog;
import com.aditya.supermarket.notification.entity.NotificationStatus;
import com.aditya.supermarket.notification.entity.NotificationType;
import com.aditya.supermarket.notification.repository.NotificationLogRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationLogServiceTest {

    @Mock
    private NotificationLogRepository repository;

    private NotificationLogService service;

    @BeforeEach
    void setUp() {
        service = new NotificationLogService(repository, properties(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createPending_savesEntryThatIsRetriedOnlyAfterTheRetryDelay() {
        when(repository.save(any(NotificationLog.class))).thenAnswer(call -> call.getArgument(0));

        NotificationLog saved = service.createPending(NotificationType.PASSWORD_RESET,
            "aditya@example.com", "Reset your password", new RenderedEmail("<p>x</p>", "x"), "42");

        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getReferenceId()).isEqualTo("42");
        assertThat(saved.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
    }

    @Test
    void recordSent_marksEntrySent() {
        NotificationLog entry = pendingLog(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(entry));

        service.recordSent(5L);

        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(entry.getSentAt()).isEqualTo(NOW);
    }

    @Test
    void recordFailure_schedulesRetry() {
        NotificationLog entry = pendingLog(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(entry));

        service.recordFailure(5L, "smtp down");

        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(entry.getAttempts()).isEqualTo(1);
        assertThat(entry.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
    }

    @Test
    void recordSent_throwsNotFound_forUnknownId() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordSent(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findDue_asksForPendingEntriesDueNowWithTheBatchLimit() {
        service.findDue(20);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            eq(NotificationStatus.PENDING), eq(NOW), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(20);
    }
}
