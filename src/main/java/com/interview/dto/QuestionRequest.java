package com.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be 200 characters or fewer")
        String title,
        @NotBlank(message = "Category is required")
        String category,
        @NotBlank(message = "Difficulty is required")
        String difficulty,
        List<String> tags,
        @NotBlank(message = "Question is required")
        @Size(max = 20000, message = "Question must be 20000 characters or fewer")
        String question,
        @Size(max = 40000, message = "Solution must be 40000 characters or fewer")
        String solution,
        String codeLanguage,
        @Size(max = 40000, message = "Code example must be 40000 characters or fewer")
        String codeExample
) {
}
