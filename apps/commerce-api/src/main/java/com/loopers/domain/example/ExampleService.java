package com.loopers.domain.example;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ExampleService {

    private final ExampleRepository exampleRepository;

    @Transactional(readOnly = true)
    public ExampleModel getExample(Long id) {
        return exampleRepository.find(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + id + "] 예시를 찾을 수 없습니다."));
    }
}
