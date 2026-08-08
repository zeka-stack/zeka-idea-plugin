plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// IntelliAI Engine 插件版本号（从 gradle.properties 中获取）
val kitVersion: String = providers.gradleProperty("kitVersion").get()
val engineVersion: String = providers.gradleProperty("engineVersion").get()
val platformVersion = providers.gradleProperty("platformVersion")

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.eclipse.org/content/groups/releases/")
    }

    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin 2.x
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // 从外部文件读取插件描述和更新记录
        description = providers.fileContents(layout.projectDirectory.file("includes/pluginDescription.html")).asText
        changeNotes = providers.fileContents(layout.projectDirectory.file("includes/pluginChanges.html")).asText

        ideaVersion {
            sinceBuild = providers.gradleProperty("platformSinceBuild")
            // 限定到已验证的 2026.2 分支，避免对尚未验证的未来 IDE 版本宣称兼容。
            untilBuild = providers.gradleProperty("platformUntilBuild")
        }
    }

    pluginVerification {
        ides {
            create("IC", "2024.2")
            create("IC", "2024.3")
            create("IC", "2025.1")
            create("IC", "2025.2")
            create("IU", "2024.2")
            create("IU", "2024.3")
            create("IU", "2025.1")
            create("IU", "2025.2")
            create("IU", "2025.3")
            create("IU", "2026.1")
            create("IU", "2026.2.0.1")
        }
    }
}

// 本地 sandbox 依赖的插件 zip（由 prepareSandbox / localPlugin 安装，不会打进本插件包）
val enginePluginZip = file("../intelli-ai-engine/build/distributions/intelli-ai-engine-$engineVersion.zip")
val checkstylePluginZip = file("../reference/checkstyle-idea/build/distributions/checkstyle-idea-26.0.0.zip")

dependencies {
    // IntelliJ Platform
    intellijPlatform {
        // create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        intellijIdea(platformVersion)

        // Bundled plugins
        bundledPlugin("com.intellij.java")
        bundledPlugin("Git4Idea")
        // 2026.2 不再通过 Git4Idea 的编译类路径传递暴露 DVCS 模块。
        if (platformVersion.get().startsWith("2026.2")) {
            bundledModule("intellij.platform.vcs.dvcs")
            bundledModule("intellij.platform.vcs.dvcs.impl")
        }

        // 依赖 IntelliAI Engine 插件
        // 注意：运行时依赖通过 plugin.xml 中的 <depends> 声明
        // 本地开发：用 localPlugin 让 prepareSandbox 装进正确 sandbox（.intellijPlatform/sandbox/.../plugins）
        // 发布到市场后，用户仍需单独安装 IntelliAI Engine；不要用 marketplace 的 plugin()
        // plugin("dev.dong4j.zeka.stack.idea.plugin.common.ai", engineVersion)
        localPlugin(enginePluginZip)
        localPlugin(checkstylePluginZip)

        zipSigner()
        pluginVerifier()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    // 编译时依赖：本地开发时，includeBuild 会自动将依赖替换为本地项目
    // 发布到市场后，编译时使用 compileOnly("dev.dong4j.zeka.stack:intelli-ai-engine:${engineVersion}")
    // 运行时依赖通过 plugin.xml 中的 <depends> 声明，用户需要单独安装 IntelliAI Engine 插件
    // 本地开发时，运行时依赖由 localPlugin + prepareSandbox 安装的 engine 插件满足
    compileOnly("dev.dong4j.zeka.stack:intelli-ai-engine:$engineVersion")

    // Idea Plugin Common 库依赖（本地库，打包时需要包含）
    implementation("dev.dong4j.zeka.stack:idea-plugin-kit:$kitVersion")

    // JGit for Git operations (排除 SLF4J 依赖，使用 IntelliJ 平台的日志框架)
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    // JGit SSH support (optional, but may be needed)
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:6.8.0.202311291450-r") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
    testImplementation("org.assertj:assertj-core:3.24.2")

    // HTTP Mock Server for testing
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")

    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")
}

tasks {
    val javaVersion = providers.gradleProperty("javaVersion").get()

    withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
        val taskNames = gradle.startParameter.taskNames
        val isBeta = taskNames.any { it.contains("publishBeta", ignoreCase = true) }
        val isDefault = taskNames.any { it.contains("publishDefault", ignoreCase = true) }
        // 注意：channels 不能为空！PublishPluginTask 通过 channels.forEach 执行上传，空列表会导致不执行任何上传
        if (isBeta) {
            channels = listOf("beta")
            hidden = false
        } else if (isDefault) {
            channels = listOf("default") // default 渠道，发布为隐藏
            hidden = true
        } else {
            channels = listOf("default") // 直接 publishPlugin 时也用 default
            hidden = false
        }
    }

    register("publishBeta") {
        group = "intellij"
        description = "Publish plugin to beta channel. Usage: ./gradlew publishBeta"
        dependsOn("publishPlugin")
    }

    register("publishDefault") {
        group = "intellij"
        description = "Publish plugin to default channel (hidden). Usage: ./gradlew publishDefault"
        dependsOn("publishPlugin")
    }

    test {
        useJUnitPlatform()
    }

    // 本地开发：先构建 intelli-ai-engine，再让 prepareSandbox 通过 localPlugin 安装
    // 注意：不要 clean，否则配置解析阶段可能找不到 zip；也不要手拷到 build/idea-sandbox（路径已过时）
    val buildAiCommonPlugin = register<Exec>("buildAiCommonPlugin") {
        description = "Build intelli-ai-engine plugin for local development"
        group = "intellij"

        val aiCommonDir = file("../intelli-ai-engine")
        workingDir = aiCommonDir
        commandLine = listOf(aiCommonDir.resolve("gradlew").absolutePath, "buildPlugin")
        // zip 已是最新则跳过，避免每次 runIde 都全量重打 engine
        inputs.dir(aiCommonDir.resolve("src"))
        inputs.file(aiCommonDir.resolve("build.gradle.kts"))
        inputs.file(aiCommonDir.resolve("gradle.properties"))
        outputs.file(enginePluginZip)
    }

    // prepareSandbox 解析 localPlugin 前必须保证 engine zip 存在
    named("prepareSandbox") {
        dependsOn(buildAiCommonPlugin)
    }
    named("prepareTestSandbox") {
        dependsOn(buildAiCommonPlugin)
    }

    runIde {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf("-Didea.kotlin.plugin.use.k2=true")
        }
        dependsOn(buildAiCommonPlugin)
        // 热更新
        // jvmArgs = listOf("-XX:AllowEnhancedClassRedefinition")
    }

}
