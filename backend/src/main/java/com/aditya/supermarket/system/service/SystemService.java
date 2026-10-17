package com.aditya.supermarket.system.service;

import com.aditya.supermarket.system.dto.PingResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Basic application information used by the frontend to verify connectivity. */
@Service
public class SystemService {

    private final String applicationName;
    private final Clock clock;

    public SystemService(@Value("${spring.application.name}") String applicationName, Clock clock) {
        this.applicationName = applicationName;
        this.clock = clock;
    }

    public PingResponse ping() {
        return new PingResponse("UP", applicationName, Instant.now(clock));
    }
}
