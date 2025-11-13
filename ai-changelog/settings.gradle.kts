rootProject.name = providers.gradleProperty("rootProjectName").orElse("ai-changelog").get()

includeBuild("../ai-common")
