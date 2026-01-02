rootProject.name = providers.gradleProperty("rootProjectName").orElse("intelli-ai-changelog").get()

includeBuild("../intelli-ai-engine")
includeBuild("../idea-plugin-kit")
