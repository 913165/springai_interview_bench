package com.interview.repository;

import com.interview.config.InterviewProperties;
import com.interview.exception.QuestionStorageException;
import com.interview.model.Question;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonQuestionRepositoryTest {

    @TempDir
    Path directory;

    @Test
    void initializesOnceAndKeepsQuestionsAfterANewRepositoryOpensTheFile() {
        Path file = directory.resolve("questions.json");
        JsonQuestionRepository first = repository(file);
        List<Question> seeded = first.findAll();
        assertTrue(seeded.size() >= 10);

        JsonQuestionRepository second = repository(file);
        assertEquals(seeded.size(), second.findAll().size());
        assertEquals(seeded.get(0).getId(), second.findAll().get(0).getId());
    }

    @Test
    void doesNotReplaceAnExistingFileOnStartup() throws Exception {
        Path file = directory.resolve("questions.json");
        Files.writeString(file, "[]", StandardCharsets.UTF_8);

        List<Question> questions = repository(file).findAll();

        assertTrue(questions.isEmpty());
        assertEquals("[]", Files.readString(file, StandardCharsets.UTF_8).trim());
    }

    @Test
    void reportsInvalidJsonAndLeavesTheFileUntouched() throws Exception {
        Path file = directory.resolve("questions.json");
        Files.writeString(file, "{not-json", StandardCharsets.UTF_8);

        QuestionStorageException error = assertThrows(QuestionStorageException.class, () -> repository(file).findAll());

        assertTrue(error.getMessage().contains("left unchanged"));
        assertEquals("{not-json", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void concurrentWritesRemainValidJson() throws Exception {
        Path file = directory.resolve("questions.json");
        Files.writeString(file, "[]", StandardCharsets.UTF_8);
        JsonQuestionRepository repository = repository(file);
        int writers = 12;
        CountDownLatch ready = new CountDownLatch(writers);
        var pool = Executors.newFixedThreadPool(writers);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < writers; index++) {
                int current = index;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    ready.await();
                    repository.mutate(questions -> {
                        questions.add(sample(current));
                        return null;
                    });
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(writers, repository.findAll().size());
        assertEquals(writers, repository(file).findAll().size());
    }

    private static Question sample(int index) {
        Question question = new Question();
        question.setId("q-" + index);
        question.setTitle("Title " + index);
        question.setCategory("SQL");
        question.setDifficulty("Beginner");
        question.setQuestion("Question " + index);
        question.setSolution("Solution");
        question.setCodeLanguage("SQL");
        question.setCodeExample("select " + index);
        question.setCreatedAt(Instant.parse("2026-11-01T00:00:00Z"));
        question.setUpdatedAt(Instant.parse("2026-11-01T00:00:00Z"));
        return question;
    }

    private static JsonQuestionRepository repository(Path file) {
        InterviewProperties properties = new InterviewProperties();
        properties.setDataFile(file.toString());
        JsonMapper mapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return new JsonQuestionRepository(properties, mapper);
    }
}
