package com.whodis.backend.photoface.entity;

import com.whodis.backend.person.entity.Person;
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
        name = "photo_faces",
        indexes = {
                @Index(name = "idx_photo_faces_photo_id", columnList = "photo_id"),
                @Index(name = "idx_photo_faces_person_id", columnList = "person_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhotoFace {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "photo_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_photo_faces_photo")
    )
    private Photo photo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "person_id",
            foreignKey = @ForeignKey(name = "fk_photo_faces_person")
    )
    private Person person;

    @Column(name = "bounding_box", nullable = false, columnDefinition = "JSONB")
    private String boundingBox;

    @Column(name = "embedding", nullable = false, columnDefinition = "BYTEA")
    private byte[] embedding;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "embedding_dimension", nullable = false)
    private int embeddingDimension;

    @Column(name = "similarity_score")
    private Float similarityScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}