dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:infra:http-client:http-client-config"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
}
