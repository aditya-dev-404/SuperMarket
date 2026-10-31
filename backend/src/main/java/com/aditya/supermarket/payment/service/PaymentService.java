package com.aditya.supermarket.payment.service;

import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.order.dto.OrderResponse;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.repository.OrderRepository;
import com.aditya.supermarket.order.service.OrderInventory;
import com.aditya.supermarket.payment.dto.PayOrderRequest;
import com.aditya.supermarket.payment.entity.Payment;
import com.aditya.supermarket.payment.gateway.PaymentGateway;
import com.aditya.supermarket.payment.gateway.PaymentResult;
import com.aditya.supermarket.payment.repository.PaymentRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pays for an online order. The order is locked while this runs, so a double click or a parallel
 * cancel can never charge twice or return the stock twice.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final OrderInventory orderInventory;
    private final Clock clock;

    @Transactional
    public OrderResponse pay(Long userId, Long orderId, PayOrderRequest request) {
        Order order = orderRepository.findForUpdateByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.isPayable()) {
            throw new BusinessRuleException("This order is not awaiting payment");
        }

        PaymentResult result = paymentGateway.charge(order.getOrderNumber(), order.getTotal(), request.outcome());
        String provider = paymentGateway.providerName();
        if (result.successful()) {
            paymentRepository.save(Payment.succeeded(order, provider, order.getTotal(), result.reference()));
            order.markPaid();
        } else {
            paymentRepository.save(Payment.failed(order, provider, order.getTotal(), result.reference()));
            order.markPaymentFailed(clock.instant());
            orderInventory.restore(order);
        }
        return OrderResponse.from(order);
    }
}
