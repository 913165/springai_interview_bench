package com.interview.repository;

import com.interview.config.InterviewProperties;
import com.interview.exception.QuestionStorageException;
import com.interview.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

@Repository
public class JsonQuestionRepository implements QuestionRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonQuestionRepository.class);

    private final Path dataFile;
    private final Path lockFile;
    private final Path backupFile;
    private final JsonMapper jsonMapper;
    private final Object monitor = new Object();

    public JsonQuestionRepository(InterviewProperties properties, JsonMapper jsonMapper) {
        if (properties.getDataFile() == null || properties.getDataFile().isBlank()) {
            throw new IllegalStateException("interview.data-file must be set");
        }
        Path configured = Path.of(properties.getDataFile());
        if (!configured.isAbsolute()) {
            configured = Path.of(System.getProperty("user.dir")).resolve(configured).normalize();
        }
        this.dataFile = configured;
        Path parent = configured.getParent();
        String fileName = configured.getFileName().toString();
        this.lockFile = parent == null
                ? Path.of(fileName + ".lock")
                : parent.resolve(fileName + ".lock");
        this.backupFile = parent == null
                ? Path.of(fileName + ".bak")
                : parent.resolve(fileName + ".bak");
        this.jsonMapper = jsonMapper;
    }

    @Override
    public List<Question> findAll() {
        return withLock(this::readQuestions);
    }

    @Override
    public <T> T mutate(Function<List<Question>, T> action) {
        return withLock(() -> {
            List<Question> questions = readQuestions();
            T result = action.apply(questions);
            writeQuestions(questions);
            return result;
        });
    }

    public static void validateSampleSet(List<Question> samples) {
        Set<String> ids = new HashSet<>();
        Set<String> titles = new HashSet<>();
        for (Question sample : samples) {
            if (sample.getId() == null || sample.getId().isBlank()) {
                throw new QuestionStorageException("A sample question is missing an id. The data file was not created.");
            }
            if (sample.getTitle() == null || sample.getTitle().isBlank()) {
                throw new QuestionStorageException("A sample question is missing a title. The data file was not created.");
            }
            if (!ids.add(sample.getId())) {
                throw new QuestionStorageException("Duplicate sample id: " + sample.getId() + ". The data file was not created.");
            }
            String titleKey = sample.getTitle().trim().toLowerCase(Locale.ROOT);
            if (!titles.add(titleKey)) {
                throw new QuestionStorageException("Duplicate sample title: " + sample.getTitle() + ". The data file was not created.");
            }
        }
    }

    private <T> T withLock(SupplierWithIo<T> work) {
        synchronized (monitor) {
            try {
                Path parent = dataFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock ignored = channel.lock()) {
                    recoverBackupIfNeeded();
                    if (!Files.exists(dataFile)) {
                        createInitialFile();
                    }
                    return work.get();
                }
            } catch (QuestionStorageException exception) {
                throw exception;
            } catch (IOException exception) {
                throw new QuestionStorageException("Could not access question file " + dataFile, exception);
            }
        }
    }

    private void recoverBackupIfNeeded() throws IOException {
        if (!Files.exists(dataFile) && Files.exists(backupFile)) {
            Files.move(backupFile, dataFile);
            log.warn("Restored question file from {}", backupFile);
        }
    }

    private void createInitialFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("seed/questions.json");
        if (!resource.exists()) {
            writeQuestions(new ArrayList<>());
            log.info("Created empty question file at {}", dataFile);
            return;
        }
        String json;
        try (InputStream input = resource.getInputStream()) {
            json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<Question> samples = parse(json);
        validateSampleSet(samples);
        writeQuestions(samples);
        log.info("Initialized question file at {} with {} sample questions", dataFile, samples.size());
    }

    private List<Question> readQuestions() {
        try {
            String json = Files.readString(dataFile, StandardCharsets.UTF_8);
            return parse(json);
        } catch (QuestionStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new QuestionStorageException("Could not read question file " + dataFile, exception);
        }
    }

    private List<Question> parse(String json) {
        String payload = json;
        if (!payload.isEmpty() && payload.charAt(0) == '\uFEFF') {
            payload = payload.substring(1);
        }
        if (payload.isBlank()) {
            throw new QuestionStorageException(
                    "Question file is empty: " + dataFile + ". The file was left unchanged.");
        }
        try {
            List<Question> parsed = jsonMapper.readValue(payload, new TypeReference<List<Question>>() {
            });
            if (parsed == null) {
                throw new QuestionStorageException(
                        "Question file must contain a JSON array: " + dataFile + ". The file was left unchanged.");
            }
            for (Question question : parsed) {
                if (question.getTags() == null) {
                    question.setTags(new ArrayList<>());
                }
            }
            return new ArrayList<>(parsed);
        } catch (QuestionStorageException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Question file could not be parsed: {}", dataFile, exception);
            throw new QuestionStorageException(
                    "Could not read " + dataFile + ". The file was left unchanged. Fix the JSON and retry.",
                    exception);
        }
    }

    private void writeQuestions(List<Question> questions) {
        Path temp = dataFile.resolveSibling(dataFile.getFileName().toString() + ".tmp");
        try {
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), questions);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            replaceAtomically(temp);
        } catch (QuestionStorageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new QuestionStorageException("Could not write question file " + dataFile, exception);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException exception) {
                log.warn("Could not remove temporary question file {}", temp, exception);
            }
        }
    }

    private void replaceAtomically(Path temp) throws IOException {
        boolean movedAside = false;
        if (Files.exists(dataFile)) {
            Files.move(dataFile, backupFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            movedAside = true;
        }
        try {
            Files.move(temp, dataFile);
        } catch (IOException exception) {
            if (movedAside && !Files.exists(dataFile) && Files.exists(backupFile)) {
                Files.move(backupFile, dataFile);
            }
            throw exception;
        }
        Files.deleteIfExists(backupFile);
    }

    @FunctionalInterface
    private interface SupplierWithIo<T> {
        T get() throws IOException;
    }

}
