package com.aditya.supermarket.order.controller;

import com.aditya.supermarket.common.dto.PagedResponse;
import com.aditya.supermarket.order.dto.UpdateSubOrderStatusRequest;
import com.aditya.supermarket.order.dto.VendorSubOrderResponse;
import com.aditya.supermarket.order.dto.VendorSubOrderSummaryResponse;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import com.aditya.supermarket.order.service.VendorOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The approved vendor's own sub-orders (the URL rule for /api/v1/vendor/** already requires the VENDOR role). */
@RestController
@RequestMapping("/api/v1/vendor/sub-orders")
@RequiredArgsConstructor
public class VendorOrderController {

    private final VendorOrderService vendorOrderService;

    @GetMapping
    public PagedResponse<VendorSubOrderSummaryResponse> list(
        @RequestParam(name = "status", required = false) SubOrderStatus status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size) {
        return vendorOrderService.list(status, page, size);
    }

    @GetMapping("/{subOrderId}")
    public VendorSubOrderResponse get(@PathVariable Long subOrderId) {
        return vendorOrderService.get(subOrderId);
    }

    @PatchMapping("/{subOrderId}/status")
    public VendorSubOrderResponse updateStatus(@PathVariable Long subOrderId,
                                               @Valid @RequestBody UpdateSubOrderStatusRequest request) {
        return vendorOrderService.updateStatus(subOrderId, request);
    }
}
