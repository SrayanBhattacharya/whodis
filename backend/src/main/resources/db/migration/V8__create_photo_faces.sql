
CREATE TABLE photo_faces (
                             id UUID PRIMARY KEY,
                             photo_id UUID NOT NULL,
                             person_id UUID,
                             bounding_box JSONB NOT NULL,
                             embedding BYTEA NOT NULL,
                             model_name VARCHAR(100) NOT NULL,
                             model_version VARCHAR(50) NOT NULL,
                             embedding_dimension INTEGER NOT NULL,
                             similarity_score REAL,
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             CONSTRAINT fk_photo_faces_photo
                                 FOREIGN KEY (photo_id)
                                     REFERENCES photos (id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_photo_faces_person
                                 FOREIGN KEY (person_id)
                                     REFERENCES people (id)
                                     ON DELETE CASCADE,

                             CONSTRAINT chk_photo_faces_embedding_dimension
                                 CHECK (embedding_dimension = 512),

                             CONSTRAINT chk_photo_faces_similarity_score
                                 CHECK (
                                     similarity_score IS NULL
                                         OR (similarity_score >= 0 AND similarity_score <= 1)
                                     )
);

CREATE INDEX idx_photo_faces_photo_id
    ON photo_faces(photo_id);

CREATE INDEX idx_photo_faces_person_id
    ON photo_faces(person_id);
