package com.loopers.support.generator;

import com.github.f4b6a3.ulid.UlidCreator;
import org.springframework.stereotype.Component;

@Component
public class ULIDGenerator {

  public String generate() {
    return UlidCreator.getUlid().toString();
  }
}
