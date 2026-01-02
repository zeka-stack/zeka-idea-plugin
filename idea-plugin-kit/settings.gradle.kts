rootProject.name = providers.gradleProperty("rootProjectName").orElse("idea-plugin-kit").get()

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

