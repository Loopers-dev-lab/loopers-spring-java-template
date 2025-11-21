package com.loopers.application.example;

import org.springframework.stereotype.Component;

import com.loopers.domain.example.ExampleModel;
import com.loopers.domain.example.ExampleService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ExampleFacade {
    private final ExampleService exampleService;

    public ExampleInfo getExample(Long id) {
        ExampleModel example = exampleService.getExample(id);
        return ExampleInfo.from(example);
    }
}
