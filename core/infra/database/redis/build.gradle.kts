plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":supports:jackson"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-redis")
}
