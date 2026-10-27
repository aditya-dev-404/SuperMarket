package com.aditya.supermarket.cart.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aditya.supermarket.cart.dto.AddCartItemRequest;
import com.aditya.supermarket.cart.dto.CartResponse;
import com.aditya.supermarket.cart.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private static final long USER_ID = 1L;

    @Mock
    private CartService cartService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(String.valueOf(USER_ID)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCart_readsTheUserIdFromTheJwt() throws Exception {
        when(cartService.getCart(USER_ID)).thenReturn(CartResponse.empty());

        mockMvc.perform(get("/api/v1/cart"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalQuantity").value(0))
            .andExpect(jsonPath("$.checkoutReady").value(false));
    }

    @Test
    void addItem_delegatesToTheService() throws Exception {
        when(cartService.addItem(USER_ID, new AddCartItemRequest(10L, 2))).thenReturn(CartResponse.empty());

        mockMvc.perform(post("/api/v1/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":10,\"quantity\":2}"))
            .andExpect(status().isOk());

        verify(cartService).addItem(USER_ID, new AddCartItemRequest(10L, 2));
    }

    @Test
    void addItem_returnsBadRequest_whenQuantityIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":10,\"quantity\":0}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void addItem_returnsBadRequest_whenVariantIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void clear_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/cart"))
            .andExpect(status().isNoContent());

        verify(cartService).clear(USER_ID);
    }
}
