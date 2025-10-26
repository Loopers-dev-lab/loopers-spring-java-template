rootProject.name = "e-commerce"

include(
    ":apps:commerce-api",
    ":apps:commerce-streamer",
    ":modules:redis",
    ":modules:kafka",
    ":supports:jackson",
    ":supports:logging",
    ":supports:monitoring",
    ":core:domain",
    ":core:infra:database:mysql:mysql-config",
    ":core:infra:database:mysql:mysql-core"
)

// configurations
pluginManagement {
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings

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
            }
        }
    }
}

include("core:service")

include("core:service:user-service")
include("core:infra:database:mysql:user-mysql")