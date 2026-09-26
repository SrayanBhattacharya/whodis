CREATE TABLE reference_images (
                                  id UUID PRIMARY KEY,
                                  person_id UUID NOT NULL,
                                  storage_key VARCHAR(255) NOT NULL,
                                  original_filename VARCHAR(255) NOT NULL,
                                  content_type VARCHAR(100) NOT NULL,
                                  file_size BIGINT NOT NULL,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                  CONSTRAINT fk_reference_images_person
                                      FOREIGN KEY (person_id)
                                          REFERENCES people (id)
                                          ON DELETE CASCADE
);

CREATE INDEX idx_reference_images_person_id
    ON reference_images (person_id);