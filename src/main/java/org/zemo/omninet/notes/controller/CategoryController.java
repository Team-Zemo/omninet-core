package org.zemo.omninet.notes.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.notes.dto.CategoryDto;
import org.zemo.omninet.notes.dto.CategoryResponse;
import org.zemo.omninet.notes.exception.ResourceNotFoundException;
import org.zemo.omninet.notes.service.CategoryService;
import org.zemo.omninet.notes.util.CommonUtil;

import java.util.List;

@Tag(name = "Category Related", description = "All the category APIs")
@RestController
@RequestMapping("/api/v1/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;


    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> saveCategory(@RequestBody CategoryDto categoryDto) {
        Boolean result = categoryService.saveCategory(categoryDto);
        if (result) {
            return CommonUtil.createBuildResponseMessage("Saved successful", HttpStatus.CREATED);
//            return new ResponseEntity<>("Saved successful", HttpStatus.CREATED);
        }

        return CommonUtil.createErrorResponseMessage("Save failed", HttpStatus.INTERNAL_SERVER_ERROR);
//        return new ResponseEntity<>("Save failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @GetMapping("/category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCategory() {


        List<CategoryDto> allCategory = categoryService.getAllCategories();

        if (CollectionUtils.isEmpty(allCategory)) {
            CommonUtil.createErrorResponseMessage("No category found", HttpStatus.NOT_FOUND);
//            return ResponseEntity.noContent().build();
        }
        return CommonUtil.createBuildResponse(allCategory, HttpStatus.OK);
//        return new ResponseEntity<>(allCategory, HttpStatus.OK);
    }

    @GetMapping("/active-category")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> getActiveCategory() {
        List<CategoryResponse> allCategory = categoryService.getActiveCategories();

        if (CollectionUtils.isEmpty(allCategory)) {
            CommonUtil.createErrorResponseMessage("No active category found", HttpStatus.NOT_FOUND);
//            return ResponseEntity.noContent().build();
        }
        return CommonUtil.createBuildResponse(allCategory, HttpStatus.OK);
//        return new ResponseEntity<>(allCategory, HttpStatus.OK);
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> getCategoryById(@PathVariable Integer id) throws ResourceNotFoundException {

        CategoryDto categoryDto = categoryService.getCategoryById(id);

        if (ObjectUtils.isEmpty(categoryDto)) {
            return CommonUtil.createErrorResponseMessage("Category not found with id : " + id, HttpStatus.NOT_FOUND);
        }
        return CommonUtil.createBuildResponse(categoryDto, HttpStatus.OK);

    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> deleteCategoryById(@PathVariable Integer id) {
        Boolean deleted = categoryService.deleteCategoryById(id);

        if (deleted) {
            return CommonUtil.createBuildResponseMessage("Deleted successful", HttpStatus.OK);
        }
        return CommonUtil.createErrorResponseMessage("Delete failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
