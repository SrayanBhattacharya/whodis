package com.whodis.backend.processingjob.entity;

import com.whodis.backend.photo.entity.Photo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "processing_jobs",
        indexes = {
                @Index(
                        name = "idx_processing_jobs_status_created_at",
                        columnList = "status, created_at"
                ),
                @Index(
                        name = "idx_processing_jobs_photo_id",
                        columnList = "photo_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingJob {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "photo_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_processing_jobs_photo")
    )
    private Photo photo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessingJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
