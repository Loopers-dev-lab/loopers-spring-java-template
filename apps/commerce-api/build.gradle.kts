dependencies {
    implementation(project(":core:infra:database:mysql:mysql-config"))
    implementation(project(":core:infra:database:redis:redis-config"))
    implementation(project(":core:infra:event:kafka-config"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    //service
    implementation(project(":core:service"))

    //domain
    implementation(project(":core:domain"))

    //http-client
    implementation(project(":core:infra:http-client:http-client-config"))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")

    // test-fixtures
    testImplementation(project(":core:infra:database:mysql:mysql-config"))
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":core:infra:database:mysql")))
    testImplementation(testFixtures(project(":core:infra:database:redis:redis-config")))
}
