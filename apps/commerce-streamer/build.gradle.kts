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

    //kafka
    implementation("org.springframework.kafka:spring-kafka")

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // querydsl
    annotationProcessor("com.querydsl:querydsl-apt::jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

}
