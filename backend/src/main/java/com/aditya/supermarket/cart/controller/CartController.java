package com.aditya.supermarket.cart.controller;

import com.aditya.supermarket.cart.dto.AddCartItemRequest;
import com.aditya.supermarket.cart.dto.CartResponse;
import com.aditya.supermarket.cart.dto.UpdateCartItemRequest;
import com.aditya.supermarket.cart.service.CartService;
import com.aditya.supermarket.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user's cart. Every call returns the full updated cart so the UI never has to merge. */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal Jwt jwt) {
        return cartService.getCart(CurrentUser.id(jwt));
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(CurrentUser.id(jwt), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateQuantity(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long itemId,
                                       @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateQuantity(CurrentUser.id(jwt), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        return cartService.removeItem(CurrentUser.id(jwt), itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal Jwt jwt) {
        cartService.clear(CurrentUser.id(jwt));
    }
}
