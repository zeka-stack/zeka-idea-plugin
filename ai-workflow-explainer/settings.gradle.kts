rootProject.name = providers.gradleProperty("rootProjectName").orElse("ai-workflow-explainer").get()

includeBuild("../ai-common")

