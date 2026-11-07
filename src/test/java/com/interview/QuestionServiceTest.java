package com.interview;

import com.interview.dto.QuestionRequest;
import com.interview.exception.DuplicateQuestionException;
import com.interview.exception.InvalidQuestionException;
import com.interview.model.Question;
import com.interview.repository.QuestionRepository;
import com.interview.service.QuestionService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionServiceTest {

    private final MemoryQuestionRepository repository = new MemoryQuestionRepository();
    private final QuestionService service = new QuestionService(repository);

    @Test
    void createsUpdatesAndFiltersWithoutDuplicatingTheId() {
        Question created = service.create(request("Hash codes", "Core Java", "Beginner", "hashCode"));
        Question sameTitleSlug = service.create(request("Hash codes!", "SQL", "Advanced", "index"));

        assertEquals("hash-codes", created.getId());
        assertEquals("hash-codes-2", sameTitleSlug.getId());

        Question updated = service.update(created.getId(), request("Hash codes revised", "Core Java", "Advanced", "hashCode"));
        assertEquals(created.getId(), updated.getId());
        assertEquals(created.getCreatedAt(), updated.getCreatedAt());
        assertEquals("Hash codes revised", updated.getTitle());
        assertEquals(2, service.list(null, null, null).size());

        List<Question> filtered = service.list("sql", "advanced", "index");
        assertEquals(1, filtered.size());
        assertEquals(sameTitleSlug.getId(), filtered.get(0).getId());
    }

    @Test
    void rejectsDuplicateTitlesAndUnknownCategories() {
        service.create(request("Same title", "Docker", "Intermediate", "jvm"));
        assertThrows(DuplicateQuestionException.class,
                () -> service.create(request(" same title ", "Docker", "Intermediate", "jvm")));
        assertThrows(InvalidQuestionException.class,
                () -> service.create(request("Other", "Cobol", "Beginner", "legacy")));
    }

    private static QuestionRequest request(String title, String category, String difficulty, String tag) {
        return new QuestionRequest(
                title,
                category,
                difficulty,
                List.of(tag),
                "Explain " + title + " with a production example.",
                "A concise solution.",
                "Java",
                "class Example {}"
        );
    }

    private static final class MemoryQuestionRepository implements QuestionRepository {
        private final List<Question> questions = new ArrayList<>();

        @Override
        public List<Question> findAll() {
            return List.copyOf(questions);
        }

        @Override
        public <T> T mutate(Function<List<Question>, T> action) {
            List<Question> working = new ArrayList<>(questions);
            T result = action.apply(working);
            questions.clear();
            questions.addAll(working);
            return result;
        }
    }
}
