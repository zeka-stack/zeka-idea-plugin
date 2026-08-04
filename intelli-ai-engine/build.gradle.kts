plugins {
    id("java")
    id("maven-publish")
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()
val kitVersion: String = providers.gradleProperty("kitVersion").get()
val platformVersion = providers.gradleProperty("platformVersion")

repositories {
    mavenLocal()
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = providers.gradleProperty("pluginGroup").get()
            artifactId = "intelli-ai-engine"
            version = providers.gradleProperty("pluginVersion").get()
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

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

dependencies {
    intellijPlatform {
        // create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        intellijIdea(platformVersion)

        bundledPlugin("com.intellij.java")
        // 2026.2 起 JCEF 从平台核心拆分为独立 bundled plugin；旧平台仍由核心直接提供。
        if (platformVersion.get().startsWith("2026.2")) {
            bundledPlugin("com.intellij.modules.jcef")
        }
        zipSigner()
        pluginVerifier()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    // Idea Plugin Common 库依赖（本地库，打包时需要包含）
    implementation("dev.dong4j.zeka.stack:idea-plugin-kit:${kitVersion}")

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

val webviewDir = layout.projectDirectory.dir("webview")
val webviewDistDir = webviewDir.dir("dist").asFile
val webviewResourcesDir = layout.projectDirectory.dir("src/main/resources/html").asFile
val engineChatFile = webviewResourcesDir.resolve("engine-chat.html")

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

    val buildWebview by registering(Exec::class) {
        workingDir = webviewDir.asFile
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            commandLine("cmd", "/c", "npm", "run", "build")
        } else {
            commandLine("npm", "run", "build")
        }
        onlyIf { webviewDir.asFile.exists() && !engineChatFile.exists() }
    }

    val copyWebview by registering(Copy::class) {
        dependsOn(buildWebview)
        from(webviewDistDir) {
            include("index.html")
            rename { "engine-chat.html" }
        }
        into(webviewResourcesDir)
        onlyIf { !engineChatFile.exists() }
    }

    named("processResources") {
        dependsOn(copyWebview)
    }

    // 热更新
    // runIde {
    //     jvmArgs = listOf("-XX:AllowEnhancedClassRedefinition")
    // }
}
