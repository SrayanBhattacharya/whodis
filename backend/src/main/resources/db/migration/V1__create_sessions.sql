CREATE TABLE sessions (
                          id UUID PRIMARY KEY,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          status VARCHAR(20) NOT NULL
);