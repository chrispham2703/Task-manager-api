package com.taskmanager.api.task;

import com.taskmanager.api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;
    private final AuthenticatedUser authenticatedUser;

    public TaskController(TaskService taskService, AuthenticatedUser authenticatedUser) {
        this.taskService = taskService;
        this.authenticatedUser = authenticatedUser;
    }

    @Operation(summary = "Create a new task", description = "Creates a task owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        Task task = taskService.createTask(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }

    @Operation(summary = "Get all tasks", description = "Returns paginated list of tasks owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @Parameter(description = "Filter by status") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) TaskPriority priority,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        Page<Task> tasks = taskService.getTasks(ownerId, status, priority, pageable);
        Page<TaskResponse> response = tasks.map(TaskResponse::from);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a task by ID", description = "Returns a specific task if owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Task not found or not owned by user")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@Parameter(description = "Task ID") @PathVariable UUID id) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        Task task = taskService.getTaskById(id, ownerId);
        return ResponseEntity.ok(TaskResponse.from(task));
    }

    @Operation(summary = "Update a task", description = "Updates a task owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Task not found or not owned by user")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @Parameter(description = "Task ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        Task task = taskService.updateTask(id, ownerId, request);
        return ResponseEntity.ok(TaskResponse.from(task));
    }

    @Operation(summary = "Delete a task", description = "Deletes a task owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Task not found or not owned by user")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@Parameter(description = "Task ID") @PathVariable UUID id) {
        UUID ownerId = authenticatedUser.getCurrentUserId();
        taskService.deleteTask(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
