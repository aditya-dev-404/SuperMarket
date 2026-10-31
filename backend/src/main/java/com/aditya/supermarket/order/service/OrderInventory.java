package com.aditya.supermarket.order.service;

import com.aditya.supermarket.inventory.service.InventoryService;
import com.aditya.supermarket.inventory.service.StockLine;
import com.aditya.supermarket.order.entity.Order;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Connects orders to stock. It translates an order into plain stock lines so the inventory package
 * never has to know what an order is.
 */
@Component
@RequiredArgsConstructor
public class OrderInventory {

    private final InventoryService inventoryService;

    /** Takes the stock for every item of the order; fails the whole checkout if any line is short. */
    public void deduct(Order order) {
        inventoryService.deduct(stockLines(order));
    }

    /** Gives the stock of every item back (cancelled order or failed payment). */
    public void restore(Order order) {
        inventoryService.restore(stockLines(order));
    }

    private static List<StockLine> stockLines(Order order) {
        return order.getSubOrders().stream()
            .flatMap(subOrder -> subOrder.getItems().stream())
            .map(item -> new StockLine(item.getVariantId(), item.getQuantity(), item.getProductName()))
            .toList();
    }
}
