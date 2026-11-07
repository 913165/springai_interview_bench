package com.interview.dto;

import java.util.List;

public record MetadataResponse(List<String> categories, List<String> difficulties, List<String> codeLanguages) {
}
