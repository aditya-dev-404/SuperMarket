package com.aditya.supermarket.order.service;

import com.aditya.supermarket.cart.dto.AddCartItemRequest;
import com.aditya.supermarket.cart.service.CartService;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.order.entity.OrderItem;
import com.aditya.supermarket.order.repository.OrderRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Puts the items of a failed-payment order back into the customer's cart. Best effort: a line that can
 * no longer be bought (variant deleted, out of stock, over the per-line limit) is skipped.
 *
 * <p>It must run after the payment transaction has committed, so the stock returned by the failed
 * payment is visible to the cart's stock check. It is deliberately not transactional itself: every
 * {@link CartService#addItem} call is its own transaction, so one rejected line cannot affect another.
 */
@Slf4j
@Service
public class OrderCartRestorer {

    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final TransactionTemplate readOnlyTransaction;

    public OrderCartRestorer(CartService cartService, OrderRepository orderRepository,
                             PlatformTransactionManager transactionManager) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    /** @return how many lines went back into the cart */
    public int restore(Long userId, Long orderId) {
        List<RestorableLine> lines = readOnlyTransaction.execute(status -> linesOf(userId, orderId));
        int restored = 0;
        for (RestorableLine line : lines) {
            if (addBack(userId, line)) {
                restored++;
            }
        }
        return restored;
    }

    private List<RestorableLine> linesOf(Long userId, Long orderId) {
        return orderRepository.findDetailedByIdAndUserId(orderId, userId)
            .map(order -> order.getSubOrders().stream()
                .flatMap(subOrder -> subOrder.getItems().stream())
                .filter(item -> item.getVariantId() != null)
                .map(RestorableLine::of)
                .toList())
            .orElse(List.of());
    }

    private boolean addBack(Long userId, RestorableLine line) {
        try {
            cartService.addItem(userId, new AddCartItemRequest(line.variantId(), line.quantity()));
            return true;
        } catch (BusinessRuleException | ResourceNotFoundException ex) {
            log.debug("Not restoring variant {} to the cart of user {}: {}", line.variantId(), userId, ex.getMessage());
            return false;
        }
    }

    private record RestorableLine(Long variantId, int quantity) {
        static RestorableLine of(OrderItem item) {
            return new RestorableLine(item.getVariantId(), item.getQuantity());
        }
    }
}
