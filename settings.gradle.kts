rootProject.name = "loopers-java-spring-template"

include(
    ":apps:commerce-api",
    ":apps:commerce-streamer",
    ":apps:pg-simulator",
    ":modules:jpa",
    ":modules:redis",
    ":modules:kafka",
    ":supports:jackson",
    ":supports:logging",
    ":supports:monitoring",
)

// configurations
pluginManagement {
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings
    val kotlinVersion: String by settings

    repositories {
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.springframework.boot" -> useVersion(springBootVersion)
                "io.spring.dependency-management" -> useVersion(springDependencyManagementVersion)
                "org.jetbrains.kotlin.jvm" -> useVersion(kotlinVersion)
                "org.jetbrains.kotlin.plugin.spring" -> useVersion(kotlinVersion)
                "org.jetbrains.kotlin.plugin.jpa" -> useVersion(kotlinVersion)
            }
        }
    }
}
