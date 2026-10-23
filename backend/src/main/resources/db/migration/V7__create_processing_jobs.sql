
CREATE TABLE processing_jobs (
                                 id UUID PRIMARY KEY,
                                 photo_id UUID NOT NULL,
                                 status VARCHAR(20) NOT NULL,
                                 attempt_count INTEGER NOT NULL DEFAULT 0,
                                 error_message TEXT,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 started_at TIMESTAMP WITH TIME ZONE,
                                 completed_at TIMESTAMP WITH TIME ZONE,

                                 CONSTRAINT fk_processing_jobs_photo
                                     FOREIGN KEY (photo_id)
                                         REFERENCES photos (id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT chk_processing_jobs_attempt_count
                                     CHECK (attempt_count >= 0),

                                 CONSTRAINT chk_processing_jobs_status
                                     CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_processing_jobs_status_created_at
    ON processing_jobs(status, created_at);

CREATE INDEX idx_processing_jobs_photo_id
    ON processing_jobs(photo_id);
