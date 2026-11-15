package com.whodis.backend.photoface.dto;

import java.util.UUID;

public record AssignFaceRequest(
        UUID personId
) {
}