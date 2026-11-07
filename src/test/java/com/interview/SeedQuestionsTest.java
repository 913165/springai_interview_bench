package com.interview;

import com.interview.model.Catalog;
import com.interview.model.Question;
import com.interview.repository.JsonQuestionRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedQuestionsTest {

    @Test
    void sampleFileCoversTheLibraryAndHasNoDuplicateIdsOrTitles() throws Exception {
        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        try (InputStream input = getClass().getResourceAsStream("/seed/questions.json")) {
            List<Question> questions = mapper.readValue(input, new TypeReference<List<Question>>() {
            });
            JsonQuestionRepository.validateSampleSet(questions);

            Set<String> categories = new HashSet<>();
            Set<String> difficulties = new HashSet<>();
            for (Question question : questions) {
                assertFalse(question.getQuestion().isBlank());
                assertFalse(question.getSolution().isBlank());
                assertTrue(Catalog.CATEGORIES.contains(question.getCategory()));
                assertTrue(Catalog.DIFFICULTIES.contains(question.getDifficulty()));
                assertTrue(Catalog.CODE_LANGUAGES.contains(question.getCodeLanguage()));
                categories.add(question.getCategory());
                difficulties.add(question.getDifficulty());
            }
            assertEquals(Set.of("Spring AI"), categories);
            assertEquals(new HashSet<>(Catalog.DIFFICULTIES), difficulties);
            assertTrue(questions.size() >= 10);
        }
    }
}
