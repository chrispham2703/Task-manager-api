package com.taskmanager.api.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query("SELECT t FROM Task t WHERE t.owner.id = :ownerId AND t.owner.status = 'ACTIVE'")
    Page<Task> findAllByOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.owner.id = :ownerId AND t.status = :status AND t.owner.status = 'ACTIVE'")
    Page<Task> findAllByOwnerIdAndStatus(
            @Param("ownerId") UUID ownerId,
            @Param("status") TaskStatus status,
            Pageable pageable
    );

    @Query("SELECT t FROM Task t WHERE t.owner.id = :ownerId AND t.priority = :priority AND t.owner.status = 'ACTIVE'")
    Page<Task> findAllByOwnerIdAndPriority(
            @Param("ownerId") UUID ownerId,
            @Param("priority") TaskPriority priority,
            Pageable pageable
    );

    @Query("SELECT t FROM Task t WHERE t.owner.id = :ownerId AND t.status = :status AND t.priority = :priority AND t.owner.status = 'ACTIVE'")
    Page<Task> findAllByOwnerIdAndStatusAndPriority(
            @Param("ownerId") UUID ownerId,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            Pageable pageable
    );

    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.owner.id = :ownerId")
    Optional<Task> findByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
