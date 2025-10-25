plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("com.github.sherter.google-java-format") version "0.9"
}

group = "dev.dong4j"
version = "1.0.0"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin 2.x
intellijPlatform {
    pluginConfiguration {
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "223"  // 2022.3
        }
    }
}

dependencies {
    // IntelliJ Platform
    intellijPlatform {
        intellijIdeaCommunity("2022.3")

        // Bundled plugins
        bundledPlugin("com.intellij.java")

        // Plugin development utilities
        instrumentationTools()

        // Test framework
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

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
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    test {
        useJUnitPlatform()
    }
}
