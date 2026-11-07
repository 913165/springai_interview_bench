package com.interview.controller;

import com.interview.dto.CategoryResponse;
import com.interview.dto.MetadataResponse;
import com.interview.service.QuestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoryController {

    private final QuestionService questionService;

    public CategoryController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        return questionService.categories();
    }

    @GetMapping("/api/metadata")
    public MetadataResponse metadata() {
        return questionService.metadata();
    }

}
