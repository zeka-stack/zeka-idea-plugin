import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.util.*

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// 读取 intelli-ai-engine 的版本号
val aiEnginePropertiesFile = file("../intelli-ai-engine/gradle.properties")
val aiEngineVersion: String? = if (aiEnginePropertiesFile.exists()) {
    val properties = Properties()
    aiEnginePropertiesFile.inputStream().use { properties.load(it) }
    properties.getProperty("pluginVersion", "1.4.0")
} else {
    "1.4.0" // 默认版本号
}

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
            untilBuild = providers.gradleProperty("platformUntilBuild")
        }
    }

    pluginVerification {
        ides {
            ide("IC", "2022.3")
            ide("IC", "2023.1")
            ide("IC", "2023.2")
            ide("IC", "2023.3")
            ide("IC", "2024.1")
            ide("IC", "2024.2")
            ide("IC", "2024.3")
            ide("IC", "2025.1")
            ide("IC", "2025.2")

            ide("IU", "2022.3")
            ide("IU", "2023.1")
            ide("IU", "2023.2")
            ide("IU", "2023.3")
            ide("IU", "2024.1")
            ide("IU", "2024.2")
            ide("IU", "2024.3")
            ide("IU", "2025.1")
            ide("IU", "2025.2")
        }
    }
}

dependencies {
    // IntelliJ Platform
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Bundled plugins
        bundledPlugin("com.intellij.java")
        // bundledPlugin("com.intellij.mermaid")
        bundledPlugin("org.intellij.plugins.markdown")

        // Plugin development utilities
        instrumentationTools()

        // Marketplace ZIP Signer for plugin signing
        zipSigner()

        // Plugin verifier for validation
        pluginVerifier()

        // Test framework
        testFramework(TestFrameworkType.Platform)
    }

    // 编译使用：本地开发时，includeBuild 会自动将 "dev.dong4j:intelli-ai-engine:${aiEngineVersion}" 替换为本地项目
    // 发布到市场后，其他开发者可以直接使用 compileOnly("dev.dong4j:intelli-ai-engine:${aiEngineVersion}")
    // 运行时依赖通过 copyAiCommonPlugin 任务安装的插件来满足
    // 注意：不要使用 implementation，会导致类加载器冲突（同一个类被两个不同的 PluginClassLoader 加载）
    compileOnly("dev.dong4j:intelli-ai-engine:${aiEngineVersion}")

    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
    testImplementation("org.assertj:assertj-core:3.24.2")

    testCompileOnly("org.projectlombok:lombok:1.18.26")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.26")
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
        channels = providers.gradleProperty("publishChannels").map { listOf(it) }
    }

    test {
        useJUnitPlatform()
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

        // 确保在 prepareSandbox 之后执行（prepareSandbox 会安装 intelli-ai-tracer 插件）
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
        dependsOn(copyAiCommonPlugin)
        // 热更新
        // jvmArgs = listOf("-XX:AllowEnhancedClassRedefinition")
    }
}

