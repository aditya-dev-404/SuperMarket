package com.aditya.supermarket.cart;

import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.catalog.entity.ProductStatus;
import com.aditya.supermarket.catalog.entity.ProductVariant;
import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.vendor.entity.Vendor;
import java.math.BigDecimal;
import org.springframework.test.util.ReflectionTestUtils;

/** Builders for entity graphs used by cart and wishlist tests (ids are assigned by reflection). */
public final class CartTestData {

    private CartTestData() {
    }

    public static void setId(Object entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    public static User user(long id) {
        User user = new User("user" + id + "@example.com", "hash", "First", "Last", Role.CUSTOMER);
        setId(user, id);
        return user;
    }

    public static Vendor approvedVendor(long vendorId, User owner) {
        Vendor vendor = new Vendor(owner, "Store " + vendorId, null, "9999999999", "Some address", null);
        vendor.approve(user(9_000));
        setId(vendor, vendorId);
        return vendor;
    }

    public static Product product(long id, Vendor vendor, ProductStatus status) {
        Product product = new Product();
        product.setVendor(vendor);
        product.setName("Product " + id);
        product.setSlug("product-" + id);
        product.setBasePrice(new BigDecimal("100.00"));
        product.setStatus(status);
        setId(product, id);
        return product;
    }

    public static ProductVariant variant(long id, Product product, int stock, BigDecimal priceOverride, boolean active) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("SKU-" + id);
        variant.setPrice(priceOverride);
        variant.setStockQuantity(stock);
        variant.setActive(active);
        setId(variant, id);
        return variant;
    }
}
