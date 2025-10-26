dependencies {
    // add-ons
    implementation(project(":core:infra:database:mysql:mysql-config"))
    implementation(project(":modules:redis"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")

    // test-fixtures
    testImplementation(testFixtures(project(":core:infra:database:mysql:mysql-core")))
}
