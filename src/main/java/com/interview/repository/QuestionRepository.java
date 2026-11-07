package com.interview.repository;

import com.interview.model.Question;

import java.util.List;
import java.util.function.Function;

public interface QuestionRepository {

    List<Question> findAll();

    <T> T mutate(Function<List<Question>, T> action);

}
