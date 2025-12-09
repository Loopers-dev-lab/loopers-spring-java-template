dependencies {
    implementation(project(":core:domain"))

    // spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-tx")
    // mysql
    runtimeOnly(project(":core:infra:database:mysql"))
    runtimeOnly(project("::core:infra:database:redis"))
    runtimeOnly(project(":core:infra:http-client"))

    implementation(project(":supports:jackson"))

    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":core:infra:database:mysql")))
    testImplementation(project(":core:infra:database:mysql:mysql-config"))
}
