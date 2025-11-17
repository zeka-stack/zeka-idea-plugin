rootProject.name = providers.gradleProperty("rootProjectName").orElse("intelli-ai-changelog").get()

includeBuild("../intelli-ai-engine")
