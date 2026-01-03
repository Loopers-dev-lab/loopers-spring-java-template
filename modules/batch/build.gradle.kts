plugins {
    `java-library`
}

dependencies {
    // Spring Batch
    api("org.springframework.boot:spring-boot-starter-batch")
    
    // JPA module
    api(project(":modules:jpa"))
    
    // Test
    testImplementation("org.springframework.batch:spring-batch-test")
}