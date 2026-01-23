package com.taskmanager.api.task;

import com.taskmanager.api.common.exception.ResourceNotFoundException;
import com.taskmanager.api.user.User;
import com.taskmanager.api.user.UserRepository;
import com.taskmanager.api.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private Task testTask;
    private final UUID userId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setStatus(UserStatus.ACTIVE);

        testTask = new Task();
        testTask.setOwner(testUser);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setStatus(TaskStatus.TODO);
        testTask.setPriority(TaskPriority.MEDIUM);
    }

    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("should create task successfully")
        void shouldCreateTaskSuccessfully() {
            // Given
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle("New Task");
            request.setDescription("New Description");
            request.setStatus(TaskStatus.TODO);
            request.setPriority(TaskPriority.HIGH);
            request.setDueDate(LocalDate.now().plusDays(7));

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Task result = taskService.createTask(userId, request);

            // Then
            assertThat(result.getTitle()).isEqualTo("New Task");
            assertThat(result.getOwner()).isEqualTo(testUser);
            assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle("New Task");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> taskService.createTask(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("should set default status and priority")
        void shouldSetDefaults() {
            // Given
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle("New Task");
            // status and priority not set

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Task result = taskService.createTask(userId, request);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(result.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        }
    }

    @Nested
    @DisplayName("getTasks")
    class GetTasks {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("should get all tasks for user")
        void shouldGetAllTasks() {
            // Given
            Page<Task> taskPage = new PageImpl<>(List.of(testTask));
            when(taskRepository.findAllByOwnerId(userId, pageable)).thenReturn(taskPage);

            // When
            Page<Task> result = taskService.getTasks(userId, null, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository).findAllByOwnerId(userId, pageable);
        }

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() {
            // Given
            Page<Task> taskPage = new PageImpl<>(List.of(testTask));
            when(taskRepository.findAllByOwnerIdAndStatus(userId, TaskStatus.TODO, pageable))
                    .thenReturn(taskPage);

            // When
            Page<Task> result = taskService.getTasks(userId, TaskStatus.TODO, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository).findAllByOwnerIdAndStatus(userId, TaskStatus.TODO, pageable);
        }

        @Test
        @DisplayName("should filter by priority")
        void shouldFilterByPriority() {
            // Given
            Page<Task> taskPage = new PageImpl<>(List.of(testTask));
            when(taskRepository.findAllByOwnerIdAndPriority(userId, TaskPriority.HIGH, pageable))
                    .thenReturn(taskPage);

            // When
            Page<Task> result = taskService.getTasks(userId, null, TaskPriority.HIGH, pageable);

            // Then
            verify(taskRepository).findAllByOwnerIdAndPriority(userId, TaskPriority.HIGH, pageable);
        }

        @Test
        @DisplayName("should filter by both status and priority")
        void shouldFilterByBoth() {
            // Given
            Page<Task> taskPage = new PageImpl<>(List.of(testTask));
            when(taskRepository.findAllByOwnerIdAndStatusAndPriority(
                    userId, TaskStatus.TODO, TaskPriority.HIGH, pageable))
                    .thenReturn(taskPage);

            // When
            Page<Task> result = taskService.getTasks(userId, TaskStatus.TODO, TaskPriority.HIGH, pageable);

            // Then
            verify(taskRepository).findAllByOwnerIdAndStatusAndPriority(
                    userId, TaskStatus.TODO, TaskPriority.HIGH, pageable);
        }
    }

    @Nested
    @DisplayName("getTaskById")
    class GetTaskById {

        @Test
        @DisplayName("should return task when found and owned by user")
        void shouldReturnTask() {
            // Given
            when(taskRepository.findByIdAndOwnerId(taskId, userId)).thenReturn(Optional.of(testTask));

            // When
            Task result = taskService.getTaskById(taskId, userId);

            // Then
            assertThat(result).isEqualTo(testTask);
        }

        @Test
        @DisplayName("should throw exception when task not found")
        void shouldThrowWhenNotFound() {
            // Given
            when(taskRepository.findByIdAndOwnerId(taskId, userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> taskService.getTaskById(taskId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Task not found");
        }
    }

    @Nested
    @DisplayName("updateTask")
    class UpdateTask {

        @Test
        @DisplayName("should update task successfully")
        void shouldUpdateTask() {
            // Given
            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setTitle("Updated Title");
            request.setStatus(TaskStatus.IN_PROGRESS);

            when(taskRepository.findByIdAndOwnerId(taskId, userId)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Task result = taskService.updateTask(taskId, userId, request);

            // Then
            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            // Given
            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setStatus(TaskStatus.DONE);
            // title not set

            when(taskRepository.findByIdAndOwnerId(taskId, userId)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Task result = taskService.updateTask(taskId, userId, request);

            // Then
            assertThat(result.getTitle()).isEqualTo("Test Task"); // unchanged
            assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE); // updated
        }
    }

    @Nested
    @DisplayName("deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("should delete task successfully")
        void shouldDeleteTask() {
            // Given
            when(taskRepository.findByIdAndOwnerId(taskId, userId)).thenReturn(Optional.of(testTask));

            // When
            taskService.deleteTask(taskId, userId);

            // Then
            verify(taskRepository).delete(testTask);
        }

        @Test
        @DisplayName("should throw exception when task not found")
        void shouldThrowWhenNotFound() {
            // Given
            when(taskRepository.findByIdAndOwnerId(taskId, userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> taskService.deleteTask(taskId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(taskRepository, never()).delete(any());
        }
    }
}
