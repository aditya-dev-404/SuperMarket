package com.aditya.supermarket.notification.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Wires the notification module: typed properties, async execution for event listeners
 * and scheduling for the retry job.
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {

    public static final String EXECUTOR_NAME = "notificationExecutor";

    /**
     * Small dedicated pool so slow SMTP calls never use (or block) request threads.
     * When the queue is full the caller runs the task itself: slower, but no email is dropped.
     */
    @Bean(name = EXECUTOR_NAME)
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("notification-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
