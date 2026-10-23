CREATE TABLE reference_embeddings (
                                      id UUID PRIMARY KEY,
                                      reference_image_id UUID NOT NULL,
                                      embedding BYTEA NOT NULL,
                                      model_name VARCHAR(100) NOT NULL,
                                      model_version VARCHAR(50) NOT NULL,
                                      embedding_dimension INTEGER NOT NULL,
                                      created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                      CONSTRAINT fk_reference_embeddings_reference_image
                                          FOREIGN KEY (reference_image_id)
                                              REFERENCES reference_images (id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT uq_reference_embeddings_reference_image
                                          UNIQUE (reference_image_id)
);

CREATE INDEX idx_reference_embeddings_reference_image_id
    ON reference_embeddings (reference_image_id);