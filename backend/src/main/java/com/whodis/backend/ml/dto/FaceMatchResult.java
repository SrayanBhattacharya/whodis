package com.whodis.backend.ml.dto;

import java.util.UUID;

public record FaceMatchResult(
        UUID personId,
        float similarity
) {}