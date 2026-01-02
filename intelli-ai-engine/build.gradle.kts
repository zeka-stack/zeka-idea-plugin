plugins {
    id("java")
    id("maven-publish")
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

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
            untilBuild = providers.gradleProperty("platformUntilBuild")
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
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        bundledPlugin("com.intellij.java")
        zipSigner()
        pluginVerifier()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    // Idea Plugin Kit 库依赖（本地库，打包时需要包含）
    implementation("dev.dong4j.zeka.stack:idea-plugin-kit:1.0.0")
    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
    testImplementation("org.assertj:assertj-core:3.24.2")

    // HTTP Mock Server for testing
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")

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

    // 热更新
    // runIde {
    //     jvmArgs = listOf("-XX:AllowEnhancedClassRedefinition")
    // }
}
