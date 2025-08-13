package org.zemo.omninet.notes.service;


import org.zemo.omninet.notes.dto.TodoDto;
import org.zemo.omninet.notes.exception.ResourceNotFoundException;

import java.util.List;

public interface TodoService {

    Boolean saveTodo(TodoDto todo) throws ResourceNotFoundException;

    TodoDto getTodoById(Integer id) throws ResourceNotFoundException;

    List<TodoDto> getTodoByUser(String id);
}
