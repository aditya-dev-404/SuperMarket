package com.aditya.supermarket.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundMapsTo404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Product not found with id 7"));
    }

    @Test
    void duplicateMapsTo409() throws Exception {
        mockMvc.perform(get("/test/duplicate")).andExpect(status().isConflict());
    }

    @Test
    void businessRuleMapsTo422() throws Exception {
        mockMvc.perform(get("/test/rule")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unexpectedErrorDoesNotLeakDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/not-found")
        String notFound() {
            throw ResourceNotFoundException.of("Product", 7);
        }

        @GetMapping("/test/duplicate")
        String duplicate() {
            throw new DuplicateResourceException("exists");
        }

        @GetMapping("/test/rule")
        String rule() {
            throw new BusinessRuleException("nope");
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("secret internals");
        }
    }
}
