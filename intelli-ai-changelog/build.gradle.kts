plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// IntelliAI Engine 插件版本号（从 gradle.properties 中获取）
val kitVersion: String = providers.gradleProperty("kitVersion").get()
val engineVersion: String = providers.gradleProperty("engineVersion").get()

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
            // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html#intellijPlatform-pluginConfiguration-ideaVersion-untilBuild
            // untilBuild = providers.gradleProperty("platformUntilBuild")
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            create("IC", "2024.2")
            create("IC", "2024.3")
            create("IC", "2025.1")
            create("IC", "2025.2")
            create("IC", "2025.3")

            create("IU", "2024.2")
            create("IU", "2024.3")
            create("IU", "2025.1")
            create("IU", "2025.2")
            create("IU", "2025.3")
        }
    }
}

dependencies {
    // IntelliJ Platform
    intellijPlatform {
        // create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        intellijIdea(providers.gradleProperty("platformVersion"))

        // Bundled plugins
        bundledPlugin("com.intellij.java")
        bundledPlugin("Git4Idea")

        // 依赖 IntelliAI Engine 插件
        // 注意：运行时依赖通过 plugin.xml 中的 <depends> 声明
        // 本地开发时，使用 copyAiCommonPlugin 任务手动安装插件
        // 发布到市场后，用户需要单独安装 IntelliAI Engine 插件
        // 不要在这里使用 plugin()，否则会导致发布到市场时找不到相关 class
        // plugin("dev.dong4j.zeka.stack.idea.plugin.common.ai", engineVersion)

        zipSigner()
        pluginVerifier()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    // 编译时依赖：本地开发时，includeBuild 会自动将依赖替换为本地项目
    // 发布到市场后，编译时使用 compileOnly("dev.dong4j.zeka.stack:intelli-ai-engine:${engineVersion}")
    // 运行时依赖通过 plugin.xml 中的 <depends> 声明，用户需要单独安装 IntelliAI Engine 插件
    // 本地开发时，运行时依赖通过 copyAiCommonPlugin 任务安装的插件来满足
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

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
    testImplementation("org.assertj:assertj-core:3.24.2")

    // HTTP Mock Server for testing
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")

    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
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

    // 本地开发：将已构建的 checkstyle-idea.zip 解压到 sandbox 的 plugins
    val copyCheckstyleIdeaPlugin = register<Copy>("copyCheckstyleIdeaPlugin") {
        description = "Unzip checkstyle-idea plugin into sandbox plugins"
        group = "intellij"

        val checkstylePluginDir = file("../reference/checkstyle-idea/build/distributions")
        val zipFiles = fileTree(checkstylePluginDir) { include("*.zip") }

        val sandboxProductDir =
            "${providers.gradleProperty("platformType").get()}-${providers.gradleProperty("platformVersion").get()}"
        val sandboxPluginsDir =
            layout.buildDirectory.dir("idea-sandbox/$sandboxProductDir/plugins").get().asFile

        doFirst {
            if (zipFiles.isEmpty) {
                throw GradleException("checkstyle-idea zip not found in ${checkstylePluginDir.absolutePath}")
            }
            sandboxPluginsDir.mkdirs()
            val existing = sandboxPluginsDir.resolve("CheckStyle-IDEA")
            if (existing.exists()) {
                existing.deleteRecursively()
            }
        }

        from(zipFiles.map { zipTree(it) })
        into(sandboxPluginsDir)
    }

    // 本地开发：构建并复制 intelli-ai-engine 插件到 sandbox
    val buildAiCommonPlugin = register<Exec>("buildAiCommonPlugin") {
        description = "Build intelli-ai-engine plugin for local development"
        group = "intellij"

        val aiCommonDir = file("../intelli-ai-engine")
        workingDir = aiCommonDir
        // 使用 intelli-ai-engine 项目的 gradlew 来执行构建（先 clean 再 buildPlugin）
        commandLine = listOf(aiCommonDir.resolve("gradlew").absolutePath, "clean", "buildPlugin")
    }

    val copyAiCommonPlugin = register<Copy>("copyAiCommonPlugin") {
        description = "Copy intelli-ai-engine plugin to sandbox for local development"
        group = "intellij"

        // 先构建 intelli-ai-engine 插件
        dependsOn(buildAiCommonPlugin)

        // 确保在 prepareSandbox 之后执行（prepareSandbox 会安装 intelli-ai-javadoc 插件）
        mustRunAfter("prepareSandbox")

        // 从 intelli-ai-engine 的构建输出复制插件
        val aiCommonPluginDir = file("../intelli-ai-engine/build/distributions")
        from(aiCommonPluginDir) {
            include("*.zip")
        }

        // 复制到 sandbox 的 plugins 目录（包含平台标识和版本，如 IC-2022.3/plugins）
        val sandboxProductDir = "${providers.gradleProperty("platformType").get()}-${providers.gradleProperty("platformVersion").get()}"
        val sandboxPluginsDir = layout.buildDirectory.dir("idea-sandbox/$sandboxProductDir/plugins").get().asFile
        sandboxPluginsDir.mkdirs()
        into(sandboxPluginsDir)

        // 解压插件 ZIP 文件
        doLast {
            val zipFiles = fileTree(sandboxPluginsDir) { include("*.zip") }
            zipFiles.forEach { zipFile ->
                val pluginDirName = zipFile.nameWithoutExtension.substringBeforeLast("-", zipFile.nameWithoutExtension)
                val targetDir = sandboxPluginsDir.resolve(pluginDirName)
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }

                copy {
                    from(zipTree(zipFile))
                    into(sandboxPluginsDir)
                }
                zipFile.delete()
            }
        }
    }

    // 在 runIde 之前执行复制任务（在 prepareSandbox 之后）
    runIde {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf("-Didea.kotlin.plugin.use.k2=true")
        }
        dependsOn(copyAiCommonPlugin, copyCheckstyleIdeaPlugin)
        // 热更新
        // jvmArgs = listOf("-XX:AllowEnhancedClassRedefinition")
    }

}
