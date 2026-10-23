ALTER TABLE photos
ADD CONSTRAINT uq_photos_storage_key UNIQUE (storage_key);