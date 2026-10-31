package com.aditya.supermarket.order.service;

import com.aditya.supermarket.address.entity.Address;
import com.aditya.supermarket.address.service.AddressService;
import com.aditya.supermarket.cart.dto.CartItemStatus;
import com.aditya.supermarket.cart.entity.CartItem;
import com.aditya.supermarket.cart.repository.CartItemRepository;
import com.aditya.supermarket.cart.service.CartItemRules;
import com.aditya.supermarket.cart.service.CartService;
import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.catalog.entity.ProductVariant;
import com.aditya.supermarket.catalog.repository.ProductImageRepository;
import com.aditya.supermarket.catalog.repository.ProductImageRepository.MainImage;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.order.config.CheckoutProperties;
import com.aditya.supermarket.order.dto.OrderResponse;
import com.aditya.supermarket.order.dto.PlaceOrderRequest;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.OrderItem;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.ShippingAddress;
import com.aditya.supermarket.order.entity.SubOrder;
import com.aditya.supermarket.order.repository.OrderRepository;
import com.aditya.supermarket.vendor.entity.Vendor;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns the user's cart into an order: re-checks every line against live data, snapshots names and
 * prices, takes the stock, and empties the cart. Everything happens in one transaction, so a failure
 * anywhere leaves the cart and the stock exactly as they were.
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final AddressService addressService;
    private final ProductImageRepository productImageRepository;
    private final OrderInventory orderInventory;
    private final OrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final CheckoutProperties checkoutProperties;

    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        Address address = addressService.getOwned(userId, request.addressId());
        List<CartItem> cartItems = cartItemRepository.findDetailedByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessRuleException("Your cart is empty");
        }
        cartItems.forEach(CheckoutService::assertAvailable);

        Order order = new Order(
            orderNumberGenerator.next(), userId, request.paymentMethod(), ShippingAddress.from(address));
        addSubOrders(order, cartItems);
        order.applyPricing(shippingFeeFor(request.paymentMethod()));

        // Before saving: a short-stock failure then rolls back without anything having been written.
        orderInventory.deduct(order);
        orderRepository.save(order);
        cartService.clear(userId);
        return OrderResponse.from(order);
    }

    /** Throws a customer-friendly message for the first line that can no longer be bought as it is in the cart. */
    private static void assertAvailable(CartItem item) {
        CartItemStatus status = CartItemRules.statusOf(item);
        if (status == CartItemStatus.AVAILABLE) {
            return;
        }
        String name = item.getVariant().getProduct().getName();
        String problem = switch (status) {
            case OUT_OF_STOCK -> "'" + name + "' is out of stock.";
            case INSUFFICIENT_STOCK -> "Only " + item.getVariant().getStockQuantity() + " of '" + name + "' left in stock.";
            default -> "'" + name + "' is no longer available.";
        };
        throw new BusinessRuleException(problem + " Please update your cart and try again.");
    }

    private void addSubOrders(Order order, List<CartItem> cartItems) {
        Map<Long, String> imageByProduct = mainImages(cartItems);
        Map<Long, List<CartItem>> itemsByVendor = cartItems.stream()
            .collect(Collectors.groupingBy(
                item -> item.getVariant().getProduct().getVendor().getId(),
                LinkedHashMap::new,
                Collectors.toList()));

        itemsByVendor.values().forEach(vendorItems -> {
            Vendor vendor = vendorItems.getFirst().getVariant().getProduct().getVendor();
            SubOrder subOrder = order.addSubOrder(vendor);
            vendorItems.forEach(item -> subOrder.addItem(toOrderItem(item, imageByProduct)));
        });
    }

    private Map<Long, String> mainImages(List<CartItem> cartItems) {
        Set<Long> productIds = cartItems.stream()
            .map(item -> item.getVariant().getProduct().getId())
            .collect(Collectors.toSet());
        return productImageRepository.findMainImages(productIds).stream()
            .collect(Collectors.toMap(MainImage::getProductId, MainImage::getUrl, (first, second) -> first));
    }

    private static OrderItem toOrderItem(CartItem cartItem, Map<Long, String> imageByProduct) {
        ProductVariant variant = cartItem.getVariant();
        Product product = variant.getProduct();
        return new OrderItem(
            variant.getId(),
            product.getId(),
            product.getName(),
            product.getSlug(),
            variant.getSku(),
            variant.getSize(),
            variant.getColour(),
            imageByProduct.get(product.getId()),
            CartItemRules.unitPrice(variant),
            cartItem.getQuantity());
    }

    private BigDecimal shippingFeeFor(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.COD ? checkoutProperties.codShippingFee() : BigDecimal.ZERO;
    }
}
