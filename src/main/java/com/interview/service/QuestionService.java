package com.interview.service;

import com.interview.dto.CategoryResponse;
import com.interview.dto.MetadataResponse;
import com.interview.dto.QuestionRequest;
import com.interview.exception.DuplicateQuestionException;
import com.interview.exception.InvalidQuestionException;
import com.interview.exception.QuestionNotFoundException;
import com.interview.model.Catalog;
import com.interview.model.Question;
import com.interview.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository repository;

    public QuestionService(QuestionRepository repository) {
        this.repository = repository;
    }

    public List<Question> list(String category, String difficulty, String search) {
        String categoryFilter = optionalChoice(category, Catalog.CATEGORIES, "Category");
        String difficultyFilter = optionalChoice(difficulty, Catalog.DIFFICULTIES, "Difficulty");
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (query.length() > 200) {
            throw new InvalidQuestionException("Search must be 200 characters or fewer.");
        }
        return repository.findAll().stream()
                .filter(question -> categoryFilter == null || categoryFilter.equals(question.getCategory()))
                .filter(question -> difficultyFilter == null || difficultyFilter.equals(question.getDifficulty()))
                .filter(question -> query.isEmpty() || matches(question, query))
                .toList();
    }

    public Question get(String id) {
        if (id == null || id.isBlank()) {
            throw new QuestionNotFoundException("Question not found.");
        }
        return repository.findAll().stream()
                .filter(question -> id.equals(question.getId()))
                .findFirst()
                .orElseThrow(() -> new QuestionNotFoundException("Question not found: " + id));
    }

    public Question create(QuestionRequest request) {
        Question draft = toQuestion(request);
        return repository.mutate(existing -> {
            ensureUniqueTitle(existing, draft.getTitle(), null);
            draft.setId(nextId(existing, draft.getTitle()));
            Instant now = Instant.now();
            draft.setCreatedAt(now);
            draft.setUpdatedAt(now);
            existing.add(draft);
            return draft;
        });
    }

    public Question update(String id, QuestionRequest request) {
        Question draft = toQuestion(request);
        return repository.mutate(existing -> {
            int index = indexOf(existing, id);
            if (index < 0) {
                throw new QuestionNotFoundException("Question not found: " + id);
            }
            ensureUniqueTitle(existing, draft.getTitle(), id);
            Question previous = existing.get(index);
            draft.setId(id);
            draft.setCreatedAt(previous.getCreatedAt() == null ? Instant.now() : previous.getCreatedAt());
            draft.setUpdatedAt(Instant.now());
            existing.set(index, draft);
            return draft;
        });
    }

    public void delete(String id) {
        repository.mutate(existing -> {
            boolean removed = existing.removeIf(question -> id.equals(question.getId()));
            if (!removed) {
                throw new QuestionNotFoundException("Question not found: " + id);
            }
            return null;
        });
    }

    public List<CategoryResponse> categories() {
        Map<String, Long> counts = repository.findAll().stream()
                .map(Question::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .collect(Collectors.groupingBy(category -> category, Collectors.counting()));
        List<CategoryResponse> responses = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String category : Catalog.CATEGORIES) {
            responses.add(new CategoryResponse(category, counts.getOrDefault(category, 0L)));
            seen.add(category);
        }
        counts.keySet().stream()
                .filter(name -> !seen.contains(name))
                .sorted()
                .forEach(name -> responses.add(new CategoryResponse(name, counts.get(name))));
        return responses;
    }

    public MetadataResponse metadata() {
        return new MetadataResponse(Catalog.CATEGORIES, Catalog.DIFFICULTIES, Catalog.CODE_LANGUAGES);
    }

    private Question toQuestion(QuestionRequest request) {
        Question question = new Question();
        question.setTitle(prose(request.title()));
        question.setCategory(requireChoice(request.category(), Catalog.CATEGORIES, "Category"));
        question.setDifficulty(requireChoice(request.difficulty(), Catalog.DIFFICULTIES, "Difficulty"));
        question.setTags(cleanTags(request.tags()));
        question.setQuestion(prose(request.question()));
        question.setSolution(prose(request.solution()));
        String code = code(request.codeExample());
        String language = request.codeLanguage();
        if ((language == null || language.isBlank()) && !code.isEmpty()) {
            throw new InvalidQuestionException("Choose a programming language for the code example.");
        }
        if (language == null || language.isBlank()) {
            language = "Plain Text";
        }
        question.setCodeLanguage(requireChoice(language, Catalog.CODE_LANGUAGES, "Programming language"));
        question.setCodeExample(code);
        return question;
    }

    private List<String> cleanTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        if (tags.size() > 12) {
            throw new InvalidQuestionException("Use 12 tags or fewer.");
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > 40) {
                throw new InvalidQuestionException("Each tag must be 40 characters or fewer.");
            }
            boolean seen = unique.stream().anyMatch(existing -> existing.equalsIgnoreCase(trimmed));
            if (!seen) {
                unique.add(trimmed);
            }
        }
        return new ArrayList<>(unique);
    }

    private void ensureUniqueTitle(List<Question> existing, String title, String currentId) {
        String needle = title.toLowerCase(Locale.ROOT);
        boolean duplicate = existing.stream().anyMatch(question ->
                question.getTitle() != null
                        && (currentId == null || !currentId.equals(question.getId()))
                        && question.getTitle().trim().toLowerCase(Locale.ROOT).equals(needle));
        if (duplicate) {
            throw new DuplicateQuestionException("A question with this title already exists.");
        }
    }

    private int indexOf(List<Question> questions, String id) {
        for (int index = 0; index < questions.size(); index++) {
            if (id.equals(questions.get(index).getId())) {
                return index;
            }
        }
        return -1;
    }

    private String nextId(List<Question> existing, String title) {
        Set<String> ids = existing.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        String base = slugify(title);
        String candidate = base;
        int suffix = 2;
        while (ids.contains(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    static String slugify(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            slug = "question";
        }
        if (slug.length() > 72) {
            slug = slug.substring(0, 72).replaceAll("-+$", "");
        }
        return slug.isBlank() ? "question" : slug;
    }

    private String requireChoice(String value, List<String> allowed, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidQuestionException(label + " is required.");
        }
        return allowed.stream()
                .filter(item -> item.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidQuestionException(
                        label + " must be one of: " + String.join(", ", allowed) + "."));
    }

    private String optionalChoice(String value, List<String> allowed, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireChoice(value, allowed, label);
    }

    private boolean matches(Question question, String query) {
        return contains(question.getTitle(), query)
                || contains(question.getQuestion(), query)
                || contains(question.getCategory(), query)
                || (question.getTags() != null && question.getTags().stream().anyMatch(tag -> contains(tag, query)));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String prose(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private String code(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.trim().isEmpty()) {
            return "";
        }
        return normalized;
    }

}
