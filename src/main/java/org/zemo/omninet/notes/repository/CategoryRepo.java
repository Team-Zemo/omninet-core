package org.zemo.omninet.notes.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.zemo.omninet.notes.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepo extends JpaRepository<Category, Integer> {


    Optional<Category> findByIdAndIsDeletedFalse(Integer id);

    List<Category> findByIsDeletedFalse();

    List<Category> findByIsActiveTrueAndIsDeletedFalse();

    boolean existsByName(String name);

}
