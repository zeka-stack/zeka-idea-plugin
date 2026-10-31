// IntelliAI MCP Server: 独立可执行 MCP 服务, 将 intelli-ai-core 的
// javadoc / changelog 生成能力以 stdio 方式暴露给外部 AI Agent
// (ZCode / Claude Code / Cursor 等), 不依赖任何 IDE 运行环境

plugins {
    id("java")
    id("application")
    // fat jar 打包: 把 core + MCP SDK + JGit 等运行时依赖合并为单文件可执行 jar
    id("com.gradleup.shadow") version "8.3.6"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("serverVersion").get()

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.toVersion(providers.gradleProperty("javaVersion").get())
    targetCompatibility = JavaVersion.toVersion(providers.gradleProperty("javaVersion").get())
}

// 纯 JVM core 模块: AI 执行链 + changelog/javadoc 生成核心与模板
dependencies {
    implementation("dev.dong4j.zeka.stack:intelli-ai-core:2026.2.1004")

    // MCP 官方 Java SDK(stdio transport), 运行时依赖 Jackson 做 JSON-RPC 编解码
    implementation("io.modelcontextprotocol.sdk:mcp:0.10.0")

    // slf4j binder: 日志必须输出到 stderr(stdout 是 JSON-RPC 协议通道)
    implementation("ch.qos.logback:logback-classic:1.4.14")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    compileOnly("org.jetbrains:annotations:26.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = providers.gradleProperty("mainClass").get()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<Test> {
        useJUnitPlatform()
    }

    // shadowJar 产物名固定, 便于 MCP 客户端配置引用(如 java -jar intelli-ai-mcp-server.jar)
    shadowJar {
        archiveBaseName = "intelli-ai-mcp-server"
        archiveClassifier = ""
        archiveVersion = ""
    }
}
