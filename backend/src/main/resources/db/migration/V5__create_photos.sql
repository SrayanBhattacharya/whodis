CREATE TABLE photos (
                        id UUID PRIMARY KEY,
                        session_id UUID NOT NULL,
                        storage_key VARCHAR(255) NOT NULL,
                        original_filename VARCHAR(255) NOT NULL,
                        content_type VARCHAR(100) NOT NULL,
                        file_size BIGINT NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                        CONSTRAINT fk_photos_session
                            FOREIGN KEY (session_id)
                                REFERENCES sessions (id)
                                ON DELETE CASCADE
);

CREATE INDEX idx_photos_session_id
    ON photos(session_id);
