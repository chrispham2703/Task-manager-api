package com.taskmanager.api.task;

import com.taskmanager.api.common.exception.ResourceNotFoundException;
import com.taskmanager.api.user.User;
import com.taskmanager.api.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Task createTask(UUID ownerId, CreateTaskRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TaskStatus status = request.getStatus() != null ? request.getStatus() : TaskStatus.TODO;
        if (status == TaskStatus.DELETED) {
            throw new IllegalArgumentException("Cannot create a task with DELETED status");
        }

        Task task = new Task();
        task.setOwner(owner);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(status);
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        task.setDueDate(request.getDueDate());

        return taskRepository.save(task);
    }

    public Page<Task> getTasks(UUID ownerId, TaskStatus status, TaskPriority priority, Pageable pageable) {
        if (status != null && priority != null) {
            return taskRepository.findAllByOwnerIdAndStatusAndPriority(ownerId, status, priority, pageable);
        } else if (status != null) {
            return taskRepository.findAllByOwnerIdAndStatus(ownerId, status, pageable);
        } else if (priority != null) {
            return taskRepository.findAllByOwnerIdAndPriority(ownerId, priority, pageable);
        }
        return taskRepository.findAllByOwnerId(ownerId, pageable);
    }

    public Task getTaskById(UUID taskId, UUID ownerId) {
        return taskRepository.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    @Transactional
    public Task updateTask(UUID taskId, UUID ownerId, UpdateTaskRequest request) {
        Task task = taskRepository.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() == TaskStatus.DELETED) {
                throw new IllegalArgumentException("Cannot set task status to DELETED; use DELETE endpoint instead");
            }
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID ownerId) {
        Task task = taskRepository.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        task.softDelete();
        taskRepository.save(task); 
    }
}
