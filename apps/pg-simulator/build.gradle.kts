plugins {
    val kotlinVersion = "2.0.20"

    id("org.jetbrains.kotlin.jvm") version(kotlinVersion)
    id("org.jetbrains.kotlin.kapt") version(kotlinVersion)
    id("org.jetbrains.kotlin.plugin.spring") version(kotlinVersion)
    id("org.jetbrains.kotlin.plugin.jpa") version(kotlinVersion)
}

kotlin {
    compilerOptions {
        jvmToolchain(21)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

dependencies {
    // add-ons
    implementation(project(":modules:jpa"))
    implementation(project(":modules:redis"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    // kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")

    // querydsl
    kapt("com.querydsl:querydsl-apt::jakarta")

    // test-fixtures
    testImplementation(testFixtures(project(":modules:jpa")))
    testImplementation(testFixtures(project(":modules:redis")))
}
