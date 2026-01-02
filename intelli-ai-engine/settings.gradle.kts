rootProject.name = providers.gradleProperty("rootProjectName").orElse("intelli-ai-engine").get()

// includeBuild("../idea-plugin-kit")
includeBuild("../idea-plugin-kit") {
    dependencySubstitution {
        substitute(module("dev.dong4j.zeka.stack:idea-plugin-kit"))
            .using(project(":"))
    }
}
