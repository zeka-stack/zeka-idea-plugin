rootProject.name = providers.gradleProperty("rootProjectName").orElse("intelli-ai-changelog").get()

includeBuild("../intelli-ai-engine")
includeBuild("../idea-plugin-kit")
// 纯 JVM 核心库: engine 的 api 依赖, 编译期类由 core 提供, 运行时类由 engine 插件 zip 携带
includeBuild("../intelli-ai-core") {
    dependencySubstitution {
        substitute(module("dev.dong4j.zeka.stack:intelli-ai-core"))
            .using(project(":"))
    }
}
