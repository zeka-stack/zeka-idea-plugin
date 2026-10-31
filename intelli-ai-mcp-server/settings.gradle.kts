rootProject.name = "intelli-ai-mcp-server"

// 纯 JVM 核心库: AI 执行链与 changelog/javadoc 生成核心, 开发期经 composite 替换引用本地源码
includeBuild("../intelli-ai-core") {
    dependencySubstitution {
        substitute(module("dev.dong4j.zeka.stack:intelli-ai-core"))
            .using(project(":"))
    }
}
