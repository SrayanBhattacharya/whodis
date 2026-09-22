CREATE TABLE people (
                        id UUID PRIMARY KEY,
                        session_id UUID NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                        CONSTRAINT fk_people_session
                            FOREIGN KEY (session_id)
                                REFERENCES sessions (id)
                                ON DELETE CASCADE,

                        CONSTRAINT uq_people_session_name
                            UNIQUE (session_id, name)
);

CREATE INDEX idx_people_session_id
    ON people (session_id);