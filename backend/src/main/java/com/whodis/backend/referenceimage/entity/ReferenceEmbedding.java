package com.whodis.backend.referenceimage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "reference_embeddings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_reference_embeddings_reference_image",
                        columnNames = "reference_image_id"
                )
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceEmbedding {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reference_image_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_reference_embeddings_reference_image"
            )
    )
    private ReferenceImage referenceImage;

    @Column(nullable = false)
    private byte[] embedding;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "embedding_dimension", nullable = false)
    private int embeddingDimension;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}