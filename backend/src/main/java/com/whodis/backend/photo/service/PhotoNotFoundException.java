package com.whodis.backend.photo.service;

import java.util.UUID;

public class PhotoNotFoundException extends RuntimeException {
    public PhotoNotFoundException(UUID photoId) {
        super("Photo not found: " + photoId);
    }
}
