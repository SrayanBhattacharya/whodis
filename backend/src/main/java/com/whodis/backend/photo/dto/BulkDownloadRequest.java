package com.whodis.backend.photo.dto;

import java.util.List;
import java.util.UUID;

public record BulkDownloadRequest(
        List<UUID> photoIds
) {}
