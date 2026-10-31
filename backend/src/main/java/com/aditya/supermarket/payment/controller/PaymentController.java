package com.aditya.supermarket.payment.controller;

import com.aditya.supermarket.common.security.CurrentUser;
import com.aditya.supermarket.order.dto.OrderResponse;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.service.OrderCartRestorer;
import com.aditya.supermarket.payment.dto.PayOrderRequest;
import com.aditya.supermarket.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mock payment for an online order. Phase 10 replaces this flow with a real provider. */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderCartRestorer orderCartRestorer;

    @PostMapping("/{orderId}/pay")
    public OrderResponse pay(@AuthenticationPrincipal Jwt jwt,
                             @PathVariable Long orderId,
                             @Valid @RequestBody PayOrderRequest request) {
        Long userId = CurrentUser.id(jwt);
        OrderResponse order = paymentService.pay(userId, orderId, request);
        if (order.paymentStatus() == PaymentStatus.PAYMENT_FAILED) {
            // After the payment transaction has committed, so the returned stock is visible to the cart.
            orderCartRestorer.restore(userId, orderId);
        }
        return order;
    }
}
