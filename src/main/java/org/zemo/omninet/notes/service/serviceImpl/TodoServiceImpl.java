package org.zemo.omninet.notes.service.serviceImpl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.zemo.omninet.notes.dto.TodoDto;
import org.zemo.omninet.notes.entity.Todo;
import org.zemo.omninet.notes.enums.TodoStatus;
import org.zemo.omninet.notes.exception.ResourceNotFoundException;
import org.zemo.omninet.notes.repository.TodoRepo;
import org.zemo.omninet.notes.service.TodoService;
import org.zemo.omninet.notes.util.Validation;

import java.util.List;

@Service
public class TodoServiceImpl implements TodoService {

    @Autowired
    private TodoRepo todoRepo;

    @Autowired
    private ModelMapper mapper;

    @Autowired
    private Validation validation;


    @Override
    public Boolean saveTodo(TodoDto todoDto) throws ResourceNotFoundException {

        //validate todo status
        validation.todoValidation(todoDto);


        Todo todo = mapper.map(todoDto, Todo.class);
        todo.setStatusId(todoDto.getStatus().getId());

        Todo save = todoRepo.save(todo);

        return !ObjectUtils.isEmpty(save);

    }

    @Override
    public TodoDto getTodoById(Integer id) throws ResourceNotFoundException {
        Todo todo = todoRepo.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("todo not found and id invalid"));
        TodoDto todoDto = mapper.map(todo, TodoDto.class);
        setStatus(todoDto, todo);
        return todoDto;
    }

    private void setStatus(TodoDto todoDto, Todo todo) {
        for (TodoStatus status : TodoStatus.values()) {
            if (status.getId().equals(todo.getStatusId())) {
                TodoDto.StatusDto statusDto = TodoDto.StatusDto.builder()
                        .id(status.getId())
                        .name(status.getName())
                        .build();
                todoDto.setStatus(statusDto);
            }
        }
    }

    @Override
    public List<TodoDto> getTodoByUser(String id) {
        String userId = id;

        return todoRepo.findByCreatedBy(userId)
                .stream()
                .map(td -> mapper.map(td, TodoDto.class))
                .toList();
    }
}
