package com.taskmanager.api.task;

import com.taskmanager.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final AuthenticatedUser authenticatedUser;

    public TaskController(TaskService taskService, AuthenticatedUser authenticatedUser) {
        this.taskService = taskService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        Task task = taskService.createTask(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        Page<Task> tasks = taskService.getTasks(ownerId, status, priority, pageable);
        Page<TaskResponse> response = tasks.map(TaskResponse::from);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID id) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        Task task = taskService.getTaskById(id, ownerId);
        return ResponseEntity.ok(TaskResponse.from(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        Task task = taskService.updateTask(id, ownerId, request);
        return ResponseEntity.ok(TaskResponse.from(task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        taskService.deleteTask(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
