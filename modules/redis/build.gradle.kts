plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-redis")

    testFixturesImplementation("com.redis:testcontainers-redis")
    testFixturesImplementation("com.fasterxml.jackson.core:jackson-databind")
}
