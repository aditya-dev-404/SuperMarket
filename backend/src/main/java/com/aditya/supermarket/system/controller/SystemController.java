package com.aditya.supermarket.system.controller;

import com.aditya.supermarket.system.dto.PingResponse;
import com.aditya.supermarket.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated endpoints for connectivity checks. */
@Tag(name = "System")
@RestController
@RequestMapping("/api/v1/public")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @Operation(summary = "Connectivity check used by the frontend")
    @GetMapping("/ping")
    public PingResponse ping() {
        return systemService.ping();
    }
}
