package com.whodis.backend.ml.client;

import com.whodis.backend.ml.dto.PhotoFaceDetectionResponse;
import com.whodis.backend.ml.dto.ReferenceEmbeddingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MlServiceClientTest {

    private MlServiceClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8001");

        server = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder.build();

        client = new MlServiceClient(restClient);
    }

    @Test
    void shouldGenerateReferenceEmbedding() {

        String response = """
                {
                    "embedding": [0.1, 0.2, 0.3],
                    "model": "buffalo_l",
                    "model_version": "2.0",
                    "dimension": 3
                }
                """;

        server.expect(requestTo(
                        "http://localhost:8001/internal/v1/embeddings/reference"
                ))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.MULTIPART_FORM_DATA
                ))
                .andRespond(withSuccess(
                        response,
                        MediaType.APPLICATION_JSON
                ));

        ReferenceEmbeddingResponse result =
                client.generateReferenceEmbedding(
                        "test-image".getBytes(),
                        "test.jpg",
                        "image/jpeg"
                );

        assertThat(result.model()).isEqualTo("buffalo_l");
        assertThat(result.modelVersion()).isEqualTo("2.0");
        assertThat(result.dimension()).isEqualTo(3);
        assertThat(result.embedding()).containsExactly(
                0.1f, 0.2f, 0.3f
        );

        server.verify();
    }

    @Test
    void shouldDetectMultipleFaces() {

        String response = """
            {
                "model": "buffalo_l",
                "model_version": "2.0",
                "dimension": 512,
                "faces": [
                    {
                        "bounding_box": {
                            "x1": 10.0,
                            "y1": 20.0,
                            "x2": 100.0,
                            "y2": 120.0
                        },
                        "det_score": 0.98,
                        "embedding": [%s]
                    },
                    {
                        "bounding_box": {
                            "x1": 150.0,
                            "y1": 30.0,
                            "x2": 240.0,
                            "y2": 130.0
                        },
                        "det_score": 0.95,
                        "embedding": [%s]
                    }
                ]
            }
            """.formatted(
                "0.1,".repeat(511) + "0.1",
                "0.2,".repeat(511) + "0.2"
        );

        server.expect(requestTo(
                        "http://localhost:8001/internal/v1/faces/detect"
                ))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.MULTIPART_FORM_DATA
                ))
                .andRespond(withSuccess(
                        response,
                        MediaType.APPLICATION_JSON
                ));

        PhotoFaceDetectionResponse result = client.detectFaces(
                "test-image".getBytes(),
                "group.jpg",
                "image/jpeg"
        );

        assertThat(result.model()).isEqualTo("buffalo_l");
        assertThat(result.modelVersion()).isEqualTo("2.0");
        assertThat(result.dimension()).isEqualTo(512);

        assertThat(result.faces()).hasSize(2);

        assertThat(result.faces().get(0).detectionScore()).isEqualTo(0.98f);
        assertThat(result.faces().get(0).boundingBox().x1()).isEqualTo(10.0f);
        assertThat(result.faces().get(0).embedding()).hasSize(512);

        assertThat(result.faces().get(1).detectionScore()).isEqualTo(0.95f);
        assertThat(result.faces().get(1).embedding()).hasSize(512);

        server.verify();
    }

    @Test
    void shouldReturnEmptyListWhenNoFacesDetected() {

        String response = """
            {
                "model": "buffalo_l",
                "model_version": "2.0",
                "dimension": 512,
                "faces": []
            }
            """;

        server.expect(requestTo(
                        "http://localhost:8001/internal/v1/faces/detect"
                ))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(
                        response,
                        MediaType.APPLICATION_JSON
                ));

        PhotoFaceDetectionResponse result = client.detectFaces(
                "test-image".getBytes(),
                "empty.jpg",
                "image/jpeg"
        );

        assertThat(result.faces()).isEmpty();

        server.verify();
    }
}