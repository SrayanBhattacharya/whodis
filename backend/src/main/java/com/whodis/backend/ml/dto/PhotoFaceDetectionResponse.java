package com.whodis.backend.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PhotoFaceDetectionResponse(
        String model,

        @JsonProperty("model_version")
        String modelVersion,

        int dimension,

        List<DetectedFace> faces
) {}
