dependencies {
    implementation(project(":core:domain"))

    // spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-tx")


    // mysql
    runtimeOnly(project(":core:infra:database:mysql:product-mysql"))
    runtimeOnly(project(":core:infra:database:mysql:user-mysql"))

    // redis
    runtimeOnly(project(":core:infra:database:redis:product-redis"))

    testImplementation(testFixtures(project(":core:infra:database:mysql:mysql-core")))
    testImplementation(testFixtures(project(":core:infra:database:redis:redis-config")))
    testImplementation(project(":core:infra:database:mysql:mysql-config"))
}
