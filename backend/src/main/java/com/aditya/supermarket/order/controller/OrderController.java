package com.aditya.supermarket.order.controller;

import com.aditya.supermarket.common.dto.PagedResponse;
import com.aditya.supermarket.common.security.CurrentUser;
import com.aditya.supermarket.order.dto.OrderResponse;
import com.aditya.supermarket.order.dto.OrderSummaryResponse;
import com.aditya.supermarket.order.dto.PlaceOrderRequest;
import com.aditya.supermarket.order.service.CheckoutService;
import com.aditya.supermarket.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in customer's orders: checkout, history, detail, cancel. */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest request) {
        return checkoutService.placeOrder(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public PagedResponse<OrderSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(name = "page", defaultValue = "0") int page,
                                                    @RequestParam(name = "size", defaultValue = "10") int size) {
        return orderService.list(CurrentUser.id(jwt), page, size);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
        return orderService.get(CurrentUser.id(jwt), orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
        return orderService.cancel(CurrentUser.id(jwt), orderId);
    }
}
