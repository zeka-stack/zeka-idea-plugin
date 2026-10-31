rootProject.name = providers.gradleProperty("rootProjectName").orElse("intelli-ai-engine").get()

// includeBuild("../idea-plugin-kit")
includeBuild("../idea-plugin-kit") {
    dependencySubstitution {
        substitute(module("dev.dong4j.zeka.stack:idea-plugin-kit"))
            .using(project(":"))
    }
}

// 纯 JVM 核心库: AI 调用链的基础模型与解析逻辑, engine 依赖它并随插件 zip 打包
includeBuild("../intelli-ai-core") {
    dependencySubstitution {
        substitute(module("dev.dong4j.zeka.stack:intelli-ai-core"))
            .using(project(":"))
    }
}
