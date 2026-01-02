rootProject.name = providers.gradleProperty("rootProjectName").orElse("intelli-ai-tracer").get()

includeBuild("../intelli-ai-engine")
includeBuild("../idea-plugin-kit")

