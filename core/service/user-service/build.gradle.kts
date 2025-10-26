dependencies {
    implementation(project(":core:domain"))

    // spring
    implementation("org.springframework.boot:spring-boot-starter")

    // mysql
    runtimeOnly(project(":core:infra:database:mysql:user-mysql"))
}
