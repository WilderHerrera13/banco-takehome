package com.bp.client;

import com.bp.client.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"clients", "movements"})
@DisplayName("F6 - Integration test of the client service")
class ClientIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("creates a client through the endpoint and stores it in the database")
    void shouldCreateClientThroughTheEndpoint() {
        Map<String, Object> body = Map.of(
                "name", "Marianela Montalvo",
                "gender", "FEMALE",
                "age", 30,
                "identification", "1712345679",
                "address", "Amazonas y NNUU",
                "phone", "097548965",
                "password", "5678",
                "active", true);

        ResponseEntity<String> response = restTemplate.postForEntity("/clientes", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Marianela Montalvo");
        assertThat(clientRepository.existsByIdentification("1712345679")).isTrue();
    }

    @Test
    @DisplayName("answers 404 with a structured error body when the client does not exist")
    void shouldAnswerNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity("/clientes/no-existe", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Client not found");
    }
}
