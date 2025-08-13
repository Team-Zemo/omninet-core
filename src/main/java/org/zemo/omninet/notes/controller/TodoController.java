package org.zemo.omninet.notes.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.notes.dto.TodoDto;
import org.zemo.omninet.notes.exception.ResourceNotFoundException;
import org.zemo.omninet.notes.service.NotesService;
import org.zemo.omninet.notes.service.TodoService;
import org.zemo.omninet.notes.util.CommonUtil;

import java.util.List;


@Tag(name = "Todo related", description = "All the todo APIs")
@RestController
@RequestMapping("api/v1/todo")
public class TodoController {


    @Autowired
    private NotesService notesService;
    @Autowired
    private TodoService todoService;

    @PostMapping("/")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> saveTodo(@RequestBody TodoDto todo) throws ResourceNotFoundException {
        Boolean save = todoService.saveTodo(todo);
        if (save) {
            return CommonUtil.createBuildResponseMessage("Todo save success", HttpStatus.CREATED);
        }
        return CommonUtil.createErrorResponseMessage("not saved", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> getTodoById(@PathVariable Integer id) throws ResourceNotFoundException {
        TodoDto save = todoService.getTodoById(id);
        return CommonUtil.createBuildResponse(save, HttpStatus.OK);

    }

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> getAllTodo() throws ResourceNotFoundException {
        List<TodoDto> save = todoService.getTodoByUser(CommonUtil.getLoggedInUser().getEmail());
        if (CollectionUtils.isEmpty(save)) {
            return ResponseEntity.noContent().build();
        }
        return CommonUtil.createBuildResponse(save, HttpStatus.OK);

    }
}
