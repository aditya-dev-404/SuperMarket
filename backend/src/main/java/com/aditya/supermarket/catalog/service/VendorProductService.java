package com.aditya.supermarket.catalog.service;

import com.aditya.supermarket.catalog.dto.*;
import com.aditya.supermarket.catalog.entity.*;
import com.aditya.supermarket.catalog.repository.CategoryRepository;
import com.aditya.supermarket.catalog.repository.ProductRepository;
import com.aditya.supermarket.catalog.repository.ProductVariantRepository;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.DuplicateResourceException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.vendor.entity.Vendor;
import com.aditya.supermarket.vendor.service.VendorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendorProductService {

    private static final int MAX_IMAGES = 8;

    private final VendorContext vendorContext;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinarySignatureService cloudinary;

    // ---------- products ----------

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> list(ProductStatus status, int page, int size) {
        Vendor vendor = vendorContext.requireApprovedVendor();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "id"));
        Page<Product> result = status == null
            ? productRepository.findByVendorId(vendor.getId(), pageable)
            : productRepository.findByVendorIdAndStatus(vendor.getId(), status, pageable);
        return result.map(ProductSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long productId) {
        return ProductResponse.from(ownedProduct(productId));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Vendor vendor = vendorContext.requireApprovedVendor();
        Product product = new Product();
        product.setVendor(vendor);
        product.setSlug(SlugUtil.uniqueSlug(request.name()));
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        Product product = ownedProduct(productId);
        apply(product, request); // slug stays stable so shared links do not break
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse changeStatus(Long productId, ProductStatus status) {
        Product product = ownedProduct(productId);
        if (status == ProductStatus.ACTIVE) {
            if (product.getVariants().stream().noneMatch(ProductVariant::isActive)) {
                throw new BusinessRuleException("Add at least one active variant before publishing");
            }
            if (product.getImages().isEmpty()) {
                throw new BusinessRuleException("Add at least one image before publishing");
            }
        }
        product.setStatus(status);
        return ProductResponse.from(product);
    }

    // ---------- variants ----------

    @Transactional
    public ProductResponse addVariant(Long productId, VariantRequest request) {
        Product product = ownedProduct(productId);
        if (variantRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new DuplicateResourceException("SKU already exists");
        }
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        applyVariant(variant, request);
        product.getVariants().add(variant);
        productRepository.flush();
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateVariant(Long productId, Long variantId, VariantRequest request) {
        Product product = ownedProduct(productId);
        ProductVariant variant = findVariant(product, variantId);
        if (!variant.getSku().equalsIgnoreCase(request.sku())
            && variantRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new DuplicateResourceException("SKU already exists");
        }
        applyVariant(variant, request);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse deleteVariant(Long productId, Long variantId) {
        Product product = ownedProduct(productId);
        ProductVariant variant = findVariant(product, variantId);
        product.getVariants().remove(variant);
        if (product.getStatus() == ProductStatus.ACTIVE
            && product.getVariants().stream().noneMatch(ProductVariant::isActive)) {
            product.setStatus(ProductStatus.DRAFT); // never leave a live product with nothing to buy
        }
        return ProductResponse.from(product);
    }

    // ---------- images ----------

    @Transactional
    public ProductResponse addImage(Long productId, ImageRequest request) {
        Product product = ownedProduct(productId);
        if (!cloudinary.isOurUrl(request.url())) {
            throw new BusinessRuleException("Image must be uploaded through our image service");
        }
        if (product.getImages().size() >= MAX_IMAGES) {
            throw new BusinessRuleException("A product can have at most " + MAX_IMAGES + " images");
        }
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setUrl(request.url());
        image.setPublicId(request.publicId());
        image.setPosition(product.getImages().size());
        product.getImages().add(image);
        productRepository.flush();
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse deleteImage(Long productId, Long imageId) {
        Product product = ownedProduct(productId);
        ProductImage image = product.getImages().stream()
            .filter(i -> i.getId().equals(imageId)).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        product.getImages().remove(image);
        for (int i = 0; i < product.getImages().size(); i++) {
            product.getImages().get(i).setPosition(i); // keep positions contiguous; first = primary
        }
        if (product.getStatus() == ProductStatus.ACTIVE && product.getImages().isEmpty()) {
            product.setStatus(ProductStatus.DRAFT);
        }
        return ProductResponse.from(product);
    }

    // ---------- helpers ----------

    private Product ownedProduct(Long productId) {
        Vendor vendor = vendorContext.requireApprovedVendor();
        return productRepository.findByIdAndVendorId(productId, vendor.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private ProductVariant findVariant(Product product, Long variantId) {
        return product.getVariants().stream()
            .filter(v -> v.getId().equals(variantId)).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
            .filter(Category::isActive)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        product.setCategory(category);
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setBasePrice(request.basePrice());
    }

    private void applyVariant(ProductVariant variant, VariantRequest request) {
        variant.setSku(request.sku().trim().toUpperCase());
        variant.setSize(blankToNull(request.size()));
        variant.setColour(blankToNull(request.colour()));
        variant.setPrice(request.price());
        variant.setStockQuantity(request.stockQuantity());
        variant.setActive(request.active());
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
