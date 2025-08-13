package org.zemo.omninet.notes.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.zemo.omninet.notes.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepo extends JpaRepository<Category, Integer> {



    List<Category> findByIsDeletedFalse();


    List<Category> findByIsActiveTrueAndIsDeletedFalseAndCreatedBy(String email);

    Optional<Category> findByIdAndCreatedBy(Integer id, String email);

    Optional<Category> findByIdAndIsDeletedFalseAndCreatedBy(Integer id, String email);

    boolean existsByNameAndCreatedByAndIsDeletedFalse(String trim, String email);
}
