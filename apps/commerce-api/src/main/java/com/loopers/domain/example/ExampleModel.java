package com.loopers.domain.example;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "example")
public class ExampleModel extends BaseEntity {

    private String name;
    private String description;

    protected ExampleModel() {}

    public ExampleModel(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.");
        }
        if (description == null || description.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "설명은 비어있을 수 없습니다.");
        }

        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void update(String newDescription) {
        if (newDescription == null || newDescription.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "설명은 비어있을 수 없습니다.");
        }
        this.description = newDescription;
    }
}
