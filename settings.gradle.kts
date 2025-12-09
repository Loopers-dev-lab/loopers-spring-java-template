rootProject.name = "e-commerce"

include(
    ":apps:commerce-api",
    ":apps:commerce-streamer",
    ":modules:kafka",
    ":supports:jackson",
    ":supports:logging",
    ":supports:monitoring",
    ":core:domain",
    ":core:infra:database:mysql:mysql-config",
    ":core:infra:database:mysql:mysql-core",
    ":core:infra:database:mysql:user-mysql",
    ":core:infra:database:mysql:product-mysql",
    ":core:infra:database:mysql:order-mysql",
    ":core:infra:database:mysql:event-mysql",
    ":core:infra:database:redis:redis-config",
    ":core:infra:database:redis:product-redis",
    ":core:infra:http-client:http-client-config",
    ":core:infra:http-client:pg-simulator-client",
    ":core:infra:http-client:data-platform-client",
    ":core:service:user-service",
    ":core:service:product-service",
    ":core:service:order-service",
    ":core:service:payment-service",
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
