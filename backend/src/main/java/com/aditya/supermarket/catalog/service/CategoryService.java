package com.aditya.supermarket.catalog.service;

import com.aditya.supermarket.catalog.dto.CategoryRequest;
import com.aditya.supermarket.catalog.dto.CategoryResponse;
import com.aditya.supermarket.catalog.entity.Category;
import com.aditya.supermarket.catalog.repository.CategoryRepository;
import com.aditya.supermarket.common.exception.DuplicateResourceException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
            .map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndParentId(name, request.parentId())) {
            throw new DuplicateResourceException("A category with this name already exists here");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(uniqueSlug(request.name()));
        category.setParent(resolveParent(request.parentId(), null));
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        String name = request.name().trim();
        Category category = find(id);
        Long currentParentId = category.getParent() == null ? null : category.getParent().getId();
        boolean changed = !category.getName().equalsIgnoreCase(name)
            || !java.util.Objects.equals(currentParentId, request.parentId());
        if (changed && categoryRepository.existsByNameIgnoreCaseAndParentId(name, request.parentId())) {
            throw new DuplicateResourceException("A category with this name already exists here");
        }
        category.setName(name);
        category.setParent(resolveParent(request.parentId(), id));
        return CategoryResponse.from(category);
    }

    /** Soft delete: products keep pointing at the category. */
    @Transactional
    public void deactivate(Long id) {
        find(id).setActive(false);
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private Category resolveParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return null;
        }
        if (parentId.equals(selfId)) {
            throw new com.aditya.supermarket.common.exception.BusinessRuleException(
                "A category cannot be its own parent");
        }
        return find(parentId);
    }

    private String uniqueSlug(String name) {
        String base = SlugUtil.slugify(name);
        String slug = base;
        int i = 2;
        while (categoryRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
