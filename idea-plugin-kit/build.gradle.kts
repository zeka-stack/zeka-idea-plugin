plugins {
    `java-library`
    id("maven-publish")
    id("org.jetbrains.intellij.platform") version "2.16.0"
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
            artifactId = "idea-plugin-kit"
            version = providers.gradleProperty("pluginVersion").get()
        }
    }
}

dependencies {
    // IntelliJ Platform API（库项目仅编译期依赖，运行时由插件提供）
    intellijPlatform {
        // create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        intellijIdea(providers.gradleProperty("platformVersion"))
    }

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("junit:junit:4.13.2")
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

    // 这是基础库模块，禁用 IntelliJ 插件相关任务，避免误当作插件运行/打包
    val disabledIntellijTasks = setOf(
        "prepareJarSearchableOptions",
        "patchPluginXml",
        "buildSearchableOptions",
        "runIde",
        "runIdeForUiTests",
        "buildPlugin",
        "verifyPlugin",
        "signPlugin",
        "publishPlugin"
    )
    matching { it.name in disabledIntellijTasks }.configureEach {
        enabled = false
    }

    test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
        }
    }
}

// 注意：
// 1. 这是一个本地库，不会发布到 Maven 仓库
// 2. 使用 includeBuild 的其他插件会自动构建并使用这个库
// 3. 使用 implementation 依赖的插件会将此库打包到插件的 lib 目录中
