rootProject.name = providers.gradleProperty("rootProjectName").orElse("idea-plugin-kit").get()

plugins {
    // 1.0.0 removes the deprecated IBM_SEMERU reference and is compatible with Gradle 9.x.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// MessageFormatter 已平移至纯 JVM core 模块, kit 编译期经 composite 替换引用, 保持包名不变
includeBuild("../intelli-ai-core") {
    dependencySubstitution {
        substitute(module("dev.dong4j.zeka.stack:intelli-ai-core"))
            .using(project(":"))
    }
}
