plugins {
    `java-library`
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":supports:jackson"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}
