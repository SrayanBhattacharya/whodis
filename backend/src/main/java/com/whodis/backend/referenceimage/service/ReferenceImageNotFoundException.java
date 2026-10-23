package com.whodis.backend.referenceimage.service;

import java.util.UUID;

public class ReferenceImageNotFoundException extends RuntimeException {
    public ReferenceImageNotFoundException(UUID imageId) {
        super("Reference image not found: " + imageId);
    }
}
