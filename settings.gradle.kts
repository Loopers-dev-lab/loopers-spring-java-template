rootProject.name = "e-commerce"

include(
    ":apps:commerce-api",
    ":apps:commerce-streamer",
    ":modules:kafka",
    ":supports:jackson",
    ":supports:logging",
    ":supports:monitoring",
    ":core:domain",
    ":core:service",
    ":core:infra:database:mysql-config",
    ":core:infra:database:mysql-core",
    ":core:infra:database:mysql",
    ":core:infra:database:redis-config",
    ":core:infra:database:redis",
    ":core:infra:http-client:http-client-config",
    ":core:infra:http-client:pg-simulator-client",
    ":core:infra:http-client:data-platform-client",
    ":core:common"
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
