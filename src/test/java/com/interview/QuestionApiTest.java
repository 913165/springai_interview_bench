package com.interview;

import com.interview.dto.QuestionRequest;
import com.interview.model.Question;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuestionApiTest {

    private static final Path DATA_FILE = createDataFile();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("interview.data-file", DATA_FILE::toString);
        registry.add("interview.admin-key", () -> "test-admin");
    }

    @LocalServerPort
    int port;

    @Test
    void publicReadsAndProtectedWritesRoundTripThroughJson() throws Exception {
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();

        List<Question> seeded = client.get()
                .uri("/api/questions?category=Spring AI&difficulty=Beginner&search=ChatClient")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        assertEquals(1, seeded.size());
        assertEquals("chatclient-fluent-api", seeded.get(0).getId());

        ResponseEntity<String> rejected = client.post()
                .uri("/api/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request("Unauthorized question"))
                .exchange((request, response) -> ResponseEntity.status(response.getStatusCode())
                        .body(new String(response.getBody().readAllBytes())));
        assertEquals(HttpStatus.UNAUTHORIZED, rejected.getStatusCode());

        QuestionRequest payload = request("How does a local JSON library survive a restart?");
        Question created = client.post()
                .uri("/api/questions")
                .header("X-Admin-Key", "test-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Question.class);
        assertEquals("how-does-a-local-json-library-survive-a-restart", created.getId());

        QuestionRequest edited = new QuestionRequest(
                "How does a local JSON library survive a restart?",
                "System Design",
                "Advanced",
                List.of("json", "files"),
                payload.question(),
                "Write to a temp file, then replace the original.",
                "Java",
                "Files.move(temp, target);"
        );
        Question updated = client.put()
                .uri("/api/questions/{id}", created.getId())
                .header("X-Admin-Key", "test-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(edited)
                .retrieve()
                .body(Question.class);
        assertEquals(created.getId(), updated.getId());
        assertEquals("Advanced", updated.getDifficulty());
        assertEquals(created.getCreatedAt(), updated.getCreatedAt());

        String stored = Files.readString(DATA_FILE);
        assertTrue(stored.contains(created.getId()));
        assertFalse(stored.contains("\"id\" : \"" + created.getId() + "-"));

        client.delete()
                .uri("/api/questions/{id}", created.getId())
                .header("X-Admin-Key", "test-admin")
                .retrieve()
                .toBodilessEntity();

        ResponseEntity<String> missing = client.get()
                .uri("/api/questions/{id}", created.getId())
                .exchange((request, response) -> ResponseEntity.status(response.getStatusCode())
                        .body(new String(response.getBody().readAllBytes())));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertTrue(missing.getBody().contains("Question not found"));
    }

    private static QuestionRequest request(String title) {
        return new QuestionRequest(
                title,
                "System Design",
                "Intermediate",
                List.of("json"),
                "Describe the storage path for " + title,
                "Keep the JSON file outside the JAR.",
                "Plain Text",
                ""
        );
    }

    private static Path createDataFile() {
        try {
            return Files.createTempDirectory("interview-api").resolve("questions.json");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
