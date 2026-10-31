package com.aditya.supermarket.order.dto;

import com.aditya.supermarket.order.entity.SubOrderStatus;
import jakarta.validation.constraints.NotNull;

/** The vendor's next step. Only the status that follows the current one is accepted. */
public record UpdateSubOrderStatusRequest(@NotNull SubOrderStatus status) {
}
