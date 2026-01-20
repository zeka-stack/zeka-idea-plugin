rootProject.name = providers.gradleProperty("rootProjectName").get()

includeBuild("../idea-plugin-kit")
includeBuild("../intelli-ai-engine")
includeBuild("../reference/checkstyle-idea") {
    dependencySubstitution {
        substitute(module("org.infernus.idea.checkstyle:checkstyle-idea"))
            .using(project(":"))
    }
}
