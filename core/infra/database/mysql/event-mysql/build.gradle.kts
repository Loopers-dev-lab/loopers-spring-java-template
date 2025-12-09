dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:infra:database:mysql:mysql-core"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
