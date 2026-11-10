package com.aditya.supermarket.order.service;

import com.aditya.supermarket.common.dto.PagedResponse;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.notification.event.SubOrderStatusChangedEvent;
import com.aditya.supermarket.order.dto.UpdateSubOrderStatusRequest;
import com.aditya.supermarket.order.dto.VendorSubOrderResponse;
import com.aditya.supermarket.order.dto.VendorSubOrderSummaryResponse;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.SubOrder;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import com.aditya.supermarket.order.repository.OrderRepository;
import com.aditya.supermarket.order.repository.SubOrderRepository;
import com.aditya.supermarket.vendor.entity.Vendor;
import com.aditya.supermarket.vendor.service.VendorContext;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The vendor's view of the orders they have to fulfil. A vendor works only on their own sub-orders
 * and moves them forward one step at a time (PLACED, PROCESSING, SHIPPED, DELIVERED).
 */
@Service
@RequiredArgsConstructor
public class VendorOrderService {

    private static final int MAX_PAGE_SIZE = 50;
    /** Everything a vendor may see; PENDING_PAYMENT is never theirs to act on. */
    private static final List<SubOrderStatus> VISIBLE_STATUSES = List.of(
        SubOrderStatus.PLACED, SubOrderStatus.PROCESSING, SubOrderStatus.SHIPPED,
        SubOrderStatus.DELIVERED, SubOrderStatus.CANCELLED);

    private final VendorContext vendorContext;
    private final SubOrderRepository subOrderRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    /** @param status optional filter; null shows every visible status, newest first */
    @Transactional(readOnly = true)
    public PagedResponse<VendorSubOrderSummaryResponse> list(SubOrderStatus status, int page, int size) {
        Vendor vendor = vendorContext.requireApprovedVendor();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        if (status == SubOrderStatus.PENDING_PAYMENT) {
            return PagedResponse.of(List.of(), safePage, safeSize, 0);
        }

        Pageable pageable = PageRequest.of(
            safePage, safeSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        List<SubOrderStatus> statuses = status == null ? VISIBLE_STATUSES : List.of(status);
        Page<SubOrder> result = subOrderRepository.findVisibleByVendor(vendor.getId(), statuses, pageable);

        List<VendorSubOrderSummaryResponse> content = result.getContent().stream()
            .map(VendorSubOrderSummaryResponse::from)
            .toList();
        return PagedResponse.of(content, safePage, safeSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public VendorSubOrderResponse get(Long subOrderId) {
        Vendor vendor = vendorContext.requireApprovedVendor();
        return subOrderRepository.findVisibleByIdAndVendor(subOrderId, vendor.getId())
            .map(VendorSubOrderResponse::from)
            .orElseThrow(VendorOrderService::notFound);
    }

    /** Moves the sub-order exactly one step forward; the whole order is locked while this runs. */
    @Transactional
    public VendorSubOrderResponse updateStatus(Long subOrderId, UpdateSubOrderStatusRequest request) {
        Vendor vendor = vendorContext.requireApprovedVendor();
        Order order = orderRepository.findForUpdateBySubOrderIdAndVendorId(subOrderId, vendor.getId())
            .orElseThrow(VendorOrderService::notFound);
        SubOrder subOrder = order.getSubOrders().stream()
            .filter(candidate -> subOrderId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow(VendorOrderService::notFound);

        subOrder.advanceTo(request.status(), clock.instant());
        if (request.status() == SubOrderStatus.SHIPPED || request.status() == SubOrderStatus.DELIVERED) {
            // Only the steps a customer cares about; PROCESSING stays silent.
            events.publishEvent(new SubOrderStatusChangedEvent(order.getId(), subOrder.getId(), request.status()));
        }
        return VendorSubOrderResponse.from(subOrder);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Order not found");
    }
}
