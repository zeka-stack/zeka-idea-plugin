// IntelliAI Core: 纯 JVM 核心库, 不依赖 IntelliJ Platform,
// 为 engine 与独立 MCP Server 提供共用的 AI 调用链基础能力

plugins {
    id("java")
    id("maven-publish")
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("coreVersion").get()

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
}

// 发布到本地 Maven 仓库, 供未启用 composite build 的工程(如独立 MCP Server 发布流程)使用;
// 插件工程间的日常引用走 includeBuild 依赖替换, 不经过 mavenLocal
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = providers.gradleProperty("pluginGroup").get()
            artifactId = "intelli-ai-core"
            version = providers.gradleProperty("coreVersion").get()
        }
    }
}

dependencies {
    // JetBrains 注解(@NotNull/@Nullable/@Nls/@PropertyKey): 仅编译期需要, 运行时无字节码依赖
    compileOnly("org.jetbrains:annotations:26.0.2")

    // POJO 上的 @Getter 等注解处理器
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // 流式响应解析(RawStreamChunk)使用 gson; 运行时由宿主(插件 zip 或 MCP Server fat jar)打包
    implementation("com.google.code.gson:gson:2.11.0")

    // Provider/Executor 使用 @Slf4j; IDE 宿主由平台日志设施桥接, MCP Server 宿主自带 binder
    implementation("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    val javaVersion = providers.gradleProperty("javaVersion").get()

    withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        options.encoding = "UTF-8"
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<Javadoc> {
        // 平移类的 javadoc 中存在指向 engine 侧类的 @link(如 ThinkingParamStrategy),
        // 纯 JVM 模块编译时解析不到, 关闭 javadoc lint 避免构建失败
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}
