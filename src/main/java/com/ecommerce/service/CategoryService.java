package com.ecommerce.service;

import com.ecommerce.dto.CategoryDto;
import com.ecommerce.dto.UpdateCategoryRequest;
import com.ecommerce.exception.EntityNotFoundException;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryDto create(CategoryDto request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Category name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        category = categoryRepository.save(category);
        log.info("Category created: {}", category.getName());

        return categoryMapper.toDto(category);
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(UUID id) {
        return categoryMapper.toDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto update(UUID id, UpdateCategoryRequest request) {
        Category category = findEntityById(id);

        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Category name already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setUpdatedAt(LocalDateTime.now());

        log.info("Category updated: {}", category.getId());
        return categoryMapper.toDto(category);
    }

    public void delete(UUID id) {
        Category category = findEntityById(id);
        categoryRepository.delete(category);
        log.info("Category deleted: {}", id);
    }

    private Category findEntityById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
    }
}
