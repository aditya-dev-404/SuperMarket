package com.aditya.supermarket.catalog.controller;

import com.aditya.supermarket.catalog.dto.*;
import com.aditya.supermarket.catalog.entity.ProductStatus;
import com.aditya.supermarket.catalog.service.CloudinarySignatureService;
import com.aditya.supermarket.catalog.service.VendorProductService;
import com.aditya.supermarket.vendor.service.VendorContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
public class VendorProductController {

    private final VendorProductService productService;
    private final CloudinarySignatureService signatureService;
    private final VendorContext vendorContext;

    @GetMapping("/products")
    public Page<ProductSummaryResponse> list(@RequestParam(required = false) ProductStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return productService.list(status, page, size);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @GetMapping("/products/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PutMapping("/products/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/products/{id}/status")
    public ProductResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        return productService.changeStatus(id, request.status());
    }

    @PostMapping("/products/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse addVariant(@PathVariable Long id, @Valid @RequestBody VariantRequest request) {
        return productService.addVariant(id, request);
    }

    @PutMapping("/products/{id}/variants/{variantId}")
    public ProductResponse updateVariant(@PathVariable Long id, @PathVariable Long variantId,
                                         @Valid @RequestBody VariantRequest request) {
        return productService.updateVariant(id, variantId, request);
    }

    @DeleteMapping("/products/{id}/variants/{variantId}")
    public ProductResponse deleteVariant(@PathVariable Long id, @PathVariable Long variantId) {
        return productService.deleteVariant(id, variantId);
    }

    @PostMapping("/products/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse addImage(@PathVariable Long id, @Valid @RequestBody ImageRequest request) {
        return productService.addImage(id, request);
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    public ProductResponse deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        return productService.deleteImage(id, imageId);
    }

    @PostMapping("/uploads/signature")
    public UploadSignatureResponse uploadSignature() {
        return signatureService.sign(vendorContext.requireApprovedVendor());
    }
}
