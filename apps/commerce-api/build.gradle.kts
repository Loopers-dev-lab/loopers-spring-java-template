dependencies {
    // add-ons
    implementation(project(":modules:jpa"))
    implementation(project(":modules:redis"))
    implementation(project(":modules:kafka"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))


    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")

    // Resilience4j (Spring Boot 3.x 기준)
    implementation("io.github.resilience4j:resilience4j-spring-boot3")

    // AOP
    implementation("org.springframework.boot:spring-boot-starter-aop")

    //Micrometer Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus")

    //Spring Cloud OpenFeign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")


    // querydsl
    annotationProcessor("com.querydsl:querydsl-apt::jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    // test-fixtures
    testImplementation(testFixtures(project(":modules:jpa")))
    testImplementation(testFixtures(project(":modules:redis")))

    // Resilience4j (Spring Boot 3.x 기준)
    implementation("io.github.resilience4j:resilience4j-spring-boot3")

    // AOP
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    //Micrometer Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus")

    //Spring Cloud OpenFeign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    //Spring Cloud CircuitBreaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
}
