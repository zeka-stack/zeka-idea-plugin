rootProject.name = providers.gradleProperty("rootProjectName").orElse("idea-plugin-kit").get()

plugins {
    // 1.0.0 removes the deprecated IBM_SEMERU reference and is compatible with Gradle 9.x.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
