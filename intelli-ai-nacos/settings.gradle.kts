rootProject.name = providers.gradleProperty("rootProjectName").get()

// 本地开发：包含 intelli-ai-engine 项目，这样可以将 "dev.dong4j:intelli-ai-engine:1.0.0" 替换为本地项目
includeBuild("../intelli-ai-engine")