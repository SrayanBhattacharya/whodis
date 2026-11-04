package com.whodis.backend.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DetectedFace(
        @JsonProperty("bounding_box")
        BoundingBox boundingBox,

        @JsonProperty("det_score")
        float detectionScore,

        List<Float> embedding
) {}
