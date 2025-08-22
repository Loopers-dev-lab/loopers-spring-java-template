plugins {
    `java-library`
}

dependencies {
    // Spring Boot & Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-web")
    
    // Spring Cloud OpenFeign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    
    // Jackson (JSON 직렬화/역직렬화)
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.apache.commons:commons-text:1.12.0")

    // Logging
    implementation("org.springframework.boot:spring-boot-starter-logging")
    
    // Resilience4j (재시도, 서킷브레이커)
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("io.github.resilience4j:resilience4j-feign")
    
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // 내부 모듈
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    
    // Test Dependencies
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
    testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
}
