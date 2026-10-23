package com.whodis.backend.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReferenceEmbeddingResponse(
        List<Float> embedding,
        String model,

        @JsonProperty("model_version")
        String modelVersion,

        int dimension
) {
}
