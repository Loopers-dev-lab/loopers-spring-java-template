package com.loopers.infrastructure.example;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.example.ExampleModel;

public interface ExampleJpaRepository extends JpaRepository<ExampleModel, Long> {
}
