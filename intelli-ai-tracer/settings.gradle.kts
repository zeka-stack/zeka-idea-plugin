rootProject.name = providers.gradleProperty("rootProjectName").orElse("intelli-ai-tracer").get()

includeBuild("../intelli-ai-engine")

