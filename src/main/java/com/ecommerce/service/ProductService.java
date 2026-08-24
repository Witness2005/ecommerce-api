package com.ecommerce.service;

import com.ecommerce.dto.CreateProductRequest;
import com.ecommerce.dto.ProductDto;
import com.ecommerce.dto.UpdateProductRequest;
import com.ecommerce.exception.EntityNotFoundException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductDto create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU already exists");
        }

        Category category = findCategoryById(request.getCategoryId());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .sku(request.getSku())
                .category(category)
                .build();

        product = productRepository.save(product);
        log.info("Product created: {}", product.getSku());

        return productMapper.toDto(product);
    }

    @Transactional(readOnly = true)
    public ProductDto getById(UUID id) {
        return productMapper.toDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> search(String name, Pageable pageable) {
        return productRepository.searchByName(name, pageable).map(productMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getByCategory(UUID categoryId, Pageable pageable) {
        findCategoryById(categoryId);
        return productRepository.findByCategoryId(categoryId, pageable).map(productMapper::toDto);
    }

    public ProductDto update(UUID id, UpdateProductRequest request) {
        Product product = findEntityById(id);
        Category category = findCategoryById(request.getCategoryId());

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setUpdatedAt(LocalDateTime.now());

        log.info("Product updated: {}", product.getId());
        return productMapper.toDto(product);
    }

    public void delete(UUID id) {
        Product product = findEntityById(id);
        productRepository.delete(product);
        log.info("Product deleted: {}", id);
    }

    private Product findEntityById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    private Category findCategoryById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));
    }
}
