package com.aditya.supermarket.inventory.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.inventory.repository.StockRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StockRepository stockRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(stockRepository);
    }

    @Test
    void deduct_takesStockInVariantIdOrder() {
        when(stockRepository.decrease(anyLong(), anyInt())).thenReturn(1);

        inventoryService.deduct(List.of(
            new StockLine(12L, 1, "Product 12"),
            new StockLine(10L, 2, "Product 10")));

        InOrder inOrder = inOrder(stockRepository);
        inOrder.verify(stockRepository).decrease(10L, 2);
        inOrder.verify(stockRepository).decrease(12L, 1);
    }

    @Test
    void deduct_failsWithTheProductName_whenThereIsNotEnoughStock() {
        when(stockRepository.decrease(10L, 2)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.deduct(List.of(
            new StockLine(10L, 2, "Blue T-Shirt"),
            new StockLine(12L, 1, "Product 12"))))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("'Blue T-Shirt' just went out of stock");
        verify(stockRepository, never()).decrease(eq(12L), anyInt());
    }

    @Test
    void restore_givesStockBack_andSkipsDeletedVariants() {
        inventoryService.restore(List.of(
            new StockLine(null, 1, "Deleted product"),
            new StockLine(10L, 2, "Product 10")));

        verify(stockRepository).increase(10L, 2);
        verifyNoMoreInteractions(stockRepository);
    }

    @Test
    void restore_givesStockBackInVariantIdOrder() {
        inventoryService.restore(List.of(
            new StockLine(12L, 1, "Product 12"),
            new StockLine(10L, 2, "Product 10")));

        InOrder inOrder = inOrder(stockRepository);
        inOrder.verify(stockRepository).increase(10L, 2);
        inOrder.verify(stockRepository).increase(12L, 1);
    }
}
