dependencies {
    implementation(project(":core:infra:database:mysql:mysql-config"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    //service
    implementation(project(":core:service"))

    //domain
    implementation(project(":core:domain"))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")

    //batch
    implementation("org.springframework.boot:spring-boot-starter-batch")

    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // test-fixtures
    testImplementation(project(":core:infra:database:mysql:mysql-config"))
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":core:infra:database:mysql")))
}
