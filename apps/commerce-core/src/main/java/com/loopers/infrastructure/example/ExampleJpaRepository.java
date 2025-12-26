package com.loopers.infrastructure.example;

import com.loopers.domain.example.ExampleModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleJpaRepository extends JpaRepository<ExampleModel, Long> {}
