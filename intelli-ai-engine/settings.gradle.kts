rootProject.name = providers.gradleProperty("rootProjectName").orElse("intelli-ai-engine").get()

includeBuild("../idea-plugin-kit")
