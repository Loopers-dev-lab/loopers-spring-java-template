plugins {
    `java-test-fixtures`
}

dependencies {
    testFixturesImplementation("org.instancio:instancio-junit:${project.properties["instancioJUnitVersion"]}")
}
