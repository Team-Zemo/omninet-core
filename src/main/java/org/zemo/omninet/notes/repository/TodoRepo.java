package org.zemo.omninet.notes.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.zemo.omninet.notes.entity.Todo;

import java.util.List;


public interface TodoRepo extends JpaRepository<Todo, Integer> {


    List<Todo> findByCreatedBy(String userId);

    List<Todo> findByStatusIdIn(List<Integer> id);
}
