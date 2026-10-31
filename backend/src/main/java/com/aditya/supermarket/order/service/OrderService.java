package com.aditya.supermarket.order.service;

import com.aditya.supermarket.common.dto.PagedResponse;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.order.dto.OrderResponse;
import com.aditya.supermarket.order.dto.OrderSummaryResponse;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.repository.OrderRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The customer's view of their own orders: history, detail and cancellation. */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final OrderInventory orderInventory;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> list(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
            safePage, safeSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        List<Long> ids = orders.getContent().stream().map(Order::getId).toList();
        // Second query loads sub-orders and vendors for just this page (no N+1, no in-memory paging).
        Map<Long, Order> detailedById = ids.isEmpty()
            ? Map.of()
            : orderRepository.findAllDetailedByIdIn(ids).stream()
            .collect(Collectors.toMap(Order::getId, Function.identity()));

        List<OrderSummaryResponse> content = ids.stream()
            .map(detailedById::get)
            .map(OrderSummaryResponse::from)
            .toList();
        return PagedResponse.of(content, safePage, safeSize, orders.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, Long orderId) {
        return orderRepository.findDetailedByIdAndUserId(orderId, userId)
            .map(OrderResponse::from)
            .orElseThrow(OrderService::notFound);
    }

    /** Cancels the whole order and returns its stock; refused once any vendor has shipped. */
    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = orderRepository.findForUpdateByIdAndUserId(orderId, userId)
            .orElseThrow(OrderService::notFound);
        order.cancel(clock.instant());
        orderInventory.restore(order);
        return OrderResponse.from(order);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Order not found");
    }
}
