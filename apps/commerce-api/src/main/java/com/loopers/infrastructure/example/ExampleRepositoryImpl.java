package com.loopers.infrastructure.example;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loopers.domain.example.ExampleModel;
import com.loopers.domain.example.ExampleRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ExampleRepositoryImpl implements ExampleRepository {
    private final ExampleJpaRepository exampleJpaRepository;

    @Override
    public Optional<ExampleModel> find(Long id) {
        return exampleJpaRepository.findById(id);
    }
}
