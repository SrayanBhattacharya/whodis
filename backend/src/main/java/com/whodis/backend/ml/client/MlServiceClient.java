package com.whodis.backend.ml.client;

import com.whodis.backend.ml.dto.ReferenceEmbeddingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class MlServiceClient {

    private final RestClient mlRestClient;

    public ReferenceEmbeddingResponse generateReferenceEmbedding(
            byte[] image,
            String filename,
            String contentType
    ) {
        ByteArrayResource resource = new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(
                MediaType.parseMediaType(contentType)
        );

        HttpEntity<ByteArrayResource> filePart =
                new HttpEntity<>(resource, fileHeaders);

        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add("file", filePart);

        return mlRestClient
                .post()
                .uri("/internal/v1/embeddings/reference")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(ReferenceEmbeddingResponse.class);
    }
}
