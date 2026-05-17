plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()
val kitVersion: String = providers.gradleProperty("kitVersion").get()

repositories {
    mavenCentral()

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


        // Marketplace ZIP Signer for plugin signing
        zipSigner()

        // Plugin verifier for validation
        pluginVerifier()

        // Test framework
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    // Idea Plugin Common 库依赖（本地库，打包时需要包含）
    implementation("dev.dong4j.zeka.stack:idea-plugin-kit:${kitVersion}")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
    testImplementation("org.assertj:assertj-core:3.24.2")

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

    // 热更新
    runIde {
        jvmArgs = listOf("-XX:AllowEnhancedClassRedefinition")
    }
}

