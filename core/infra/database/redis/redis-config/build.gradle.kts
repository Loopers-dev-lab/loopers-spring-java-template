plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testFixturesImplementation("com.redis:testcontainers-redis")
}
