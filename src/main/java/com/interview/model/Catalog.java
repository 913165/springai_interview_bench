package com.interview.model;

import java.util.List;

public final class Catalog {

    public static final List<String> CATEGORIES = List.of(
            "Core Java",
            "Java Collections",
            "Multithreading",
            "Spring Boot",
            "Spring Security",
            "Spring AI",
            "Microservices",
            "System Design",
            "Design Patterns",
            "SQL",
            "REST APIs",
            "Docker",
            "Kubernetes"
    );

    public static final List<String> DIFFICULTIES = List.of(
            "Beginner",
            "Intermediate",
            "Advanced",
            "Principal Architect"
    );

    public static final List<String> CODE_LANGUAGES = List.of(
            "Java",
            "Python",
            "JavaScript",
            "TypeScript",
            "SQL",
            "XML",
            "JSON",
            "YAML",
            "Bash",
            "HTML",
            "CSS",
            "Plain Text"
    );

    private Catalog() {
    }

}
