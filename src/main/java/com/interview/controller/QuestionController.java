package com.interview.controller;

import com.interview.dto.QuestionRequest;
import com.interview.model.Question;
import com.interview.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = "/api/questions", produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public List<Question> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String search) {
        return questionService.list(category, difficulty, search);
    }

    @GetMapping("/{id}")
    public Question get(@PathVariable String id) {
        return questionService.get(id);
    }

    @PostMapping
    public ResponseEntity<Question> create(@Valid @RequestBody QuestionRequest request) {
        Question created = questionService.create(request);
        return ResponseEntity.created(URI.create("/api/questions/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public Question update(@PathVariable String id, @Valid @RequestBody QuestionRequest request) {
        return questionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
