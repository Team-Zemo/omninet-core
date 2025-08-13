package org.zemo.omninet.notes.service;


import org.zemo.omninet.notes.dto.CategoryDto;
import org.zemo.omninet.notes.dto.CategoryResponse;
import org.zemo.omninet.notes.exception.ResourceNotFoundException;

import java.util.List;

public interface CategoryService {

    Boolean saveCategory(CategoryDto categoryDto);

    List<CategoryDto> getAllCategories();


    List<CategoryResponse> getActiveCategories();

    CategoryDto getCategoryById(Integer id) throws ResourceNotFoundException;

    Boolean deleteCategoryById(Integer id);
}
