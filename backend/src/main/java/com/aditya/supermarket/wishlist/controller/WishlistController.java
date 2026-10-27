package com.aditya.supermarket.wishlist.controller;

import com.aditya.supermarket.cart.dto.CartResponse;
import com.aditya.supermarket.common.dto.PagedResponse;
import com.aditya.supermarket.common.security.CurrentUser;
import com.aditya.supermarket.wishlist.dto.AddWishlistItemRequest;
import com.aditya.supermarket.wishlist.dto.MoveToCartRequest;
import com.aditya.supermarket.wishlist.dto.WishlistItemResponse;
import com.aditya.supermarket.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user's wishlist. */
@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public PagedResponse<WishlistItemResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "15") int size) {
        Page<WishlistItemResponse> result = wishlistService.list(CurrentUser.id(jwt), page, size);
        return PagedResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @GetMapping("/ids")
    public List<Long> productIds(@AuthenticationPrincipal Jwt jwt) {
        return wishlistService.productIds(CurrentUser.id(jwt));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddWishlistItemRequest request) {
        wishlistService.add(CurrentUser.id(jwt), request.productId());
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        wishlistService.remove(CurrentUser.id(jwt), productId);
    }

    @PostMapping("/items/{productId}/move-to-cart")
    public CartResponse moveToCart(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable Long productId,
                                   @Valid @RequestBody MoveToCartRequest request) {
        return wishlistService.moveToCart(CurrentUser.id(jwt), productId, request);
    }
}
