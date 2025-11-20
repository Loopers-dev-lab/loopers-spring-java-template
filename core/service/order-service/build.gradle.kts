dependencies {
    implementation(project(":core:domain"))

    // spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-tx")
    // mysql
    runtimeOnly(project(":core:infra:database:mysql:product-mysql"))
    runtimeOnly(project(":core:infra:database:mysql:user-mysql"))
    runtimeOnly(project(":core:infra:database:mysql:order-mysql"))

    testImplementation(testFixtures(project(":core:infra:database:mysql:mysql-core")))
    testImplementation(project(":core:infra:database:mysql:mysql-config"))
}
