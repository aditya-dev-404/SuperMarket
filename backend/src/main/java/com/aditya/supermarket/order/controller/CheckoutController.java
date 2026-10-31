package com.aditya.supermarket.order.controller;

import com.aditya.supermarket.order.config.CheckoutProperties;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Checkout settings the page needs before an order exists. */
@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutProperties checkoutProperties;

    @GetMapping("/options")
    public CheckoutOptionsResponse options() {
        return new CheckoutOptionsResponse(checkoutProperties.codShippingFee());
    }

    /** @param codShippingFee flat fee added to Cash on Delivery orders; online orders ship free */
    public record CheckoutOptionsResponse(BigDecimal codShippingFee) {
    }
}
