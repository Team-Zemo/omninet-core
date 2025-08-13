package org.zemo.omninet.notes.service.serviceImpl;


import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.zemo.omninet.notes.dto.CategoryDto;
import org.zemo.omninet.notes.dto.CategoryResponse;
import org.zemo.omninet.notes.entity.Category;
import org.zemo.omninet.notes.exception.ExistDataException;
import org.zemo.omninet.notes.exception.ResourceNotFoundException;
import org.zemo.omninet.notes.repository.CategoryRepo;
import org.zemo.omninet.notes.service.CategoryService;
import org.zemo.omninet.notes.util.Validation;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private ModelMapper mapper;


    @Autowired
    private Validation validation;


    @Override
    public Boolean saveCategory(CategoryDto categoryDto) {


        //validation checking  --> handle
        validation.categoryValidation(categoryDto);


        // check exist before it so throw an error
        boolean exist = categoryRepo.existsByName(categoryDto.getName().trim());
        if (exist) {
            throw new ExistDataException("Category already exists");
        }


        //mapper  -> use
        Category category = mapper.map(categoryDto, Category.class);

        if (ObjectUtils.isEmpty(categoryDto.getId())) {
            //internally add
            category.setIsDeleted(false);
//            category.setCreatedBy(1);   // user id put
//            category.setCreatedDate(new Date());
        } else {
            // update request
            updateCategory(category);
        }

        return !ObjectUtils.isEmpty(categoryRepo.save(category));
    }

    private void updateCategory(Category category) {
        Optional<Category> findById = categoryRepo.findById(category.getId());
        if (findById.isPresent()) {
            Category existCategory = findById.get();
            category.setCreatedBy(existCategory.getCreatedBy());
            category.setCreatedDate(existCategory.getCreatedDate());
            category.setIsDeleted(false);

            //can be done through auditing now
//            category.setUpdatedBy(1);
//            category.setUpdatedDate(new Date());

        }

    }

    @Override
    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepo.findByIsDeletedFalse();

        List<CategoryDto> categoriesDto = categories.stream()
                .map(category -> mapper.map(category, CategoryDto.class))  // map each item
                .toList();

        return categoriesDto;
    }

    @Override
    public List<CategoryResponse> getActiveCategories() {
        List<Category> categories = categoryRepo.findByIsActiveTrueAndIsDeletedFalse();

        List<CategoryResponse> categoryResponses = categories.stream()
                .map(category -> mapper.map(category, CategoryResponse.class))  // map each item
                .toList();

        return categoryResponses;
    }

    @Override
    public CategoryDto getCategoryById(Integer id) throws ResourceNotFoundException {
        Category category = categoryRepo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with the id " + id));

        return mapper.map(category, CategoryDto.class);
    }


    @Override
    public Boolean deleteCategoryById(Integer id) {
        Optional<Category> category = categoryRepo.findById(id);

        if (category.isPresent()) {
            category.get().setIsDeleted(true);
            categoryRepo.save(category.get());
            return true;
        }
        return false;
    }


}

