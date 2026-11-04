package com.whodis.backend.ml.service;

import com.whodis.backend.ml.dto.FaceMatchResult;
import com.whodis.backend.referenceimage.entity.ReferenceEmbedding;
import com.whodis.backend.referenceimage.service.EmbeddingCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaceMatchingService {

    @Value("${whodis.face-matching.similarity-threshold:0.65}")
    private float similarityThreshold;

    @Value("${whodis.face-matching.minimum-margin:0.05}")
    private float minimumMargin;

    public FaceMatchResult findBestMatch(
            List<Float> detectedEmbedding,
            List<ReferenceEmbedding> referenceEmbeddings
    ) {
        float[] detected = toArray(detectedEmbedding);

        Map<UUID, Float> bestScores = new HashMap<>();

        for (ReferenceEmbedding reference : referenceEmbeddings) {
            float[] stored = EmbeddingCodec.decode(reference.getEmbedding());

            if (detected.length != stored.length) {
                continue;
            }

            float similarity = cosineSimilarity(detected, stored);

            UUID personId = reference
                    .getReferenceImage()
                    .getPerson()
                    .getId();

            bestScores.merge(personId, similarity, Math::max);
        }

        List<Map.Entry<UUID, Float>> ranked = bestScores.entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                .toList();

        if (ranked.isEmpty()) {
            return null;
        }

        Map.Entry<UUID, Float> best = ranked.get(0);

        if (best.getValue() < similarityThreshold) {
            return null;
        }

        if (ranked.size() > 1) {
            float margin = best.getValue() - ranked.get(1).getValue();

            if (margin < minimumMargin) {
                return null;
            }
        }

        return new FaceMatchResult(best.getKey(), best.getValue());
    }

    private float[] toArray(List<Float> embedding) {
        float[] result = new float[embedding.size()];

        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i);
        }

        return result;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0f;
        float normA = 0f;
        float normB = 0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0f || normB == 0f) {
            return 0f;
        }

        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}