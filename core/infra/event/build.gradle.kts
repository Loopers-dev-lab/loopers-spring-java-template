plugins {
    `java-library`
}

dependencies {
    implementation(project(":core:domain"))
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter")
}
