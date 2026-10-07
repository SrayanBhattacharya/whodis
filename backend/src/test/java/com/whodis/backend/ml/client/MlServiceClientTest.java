package com.whodis.backend.ml.client;

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
}