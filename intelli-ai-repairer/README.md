# IntelliAI Repairer

面向 Checkstyle/PMD 违规的 AI 修复插件，依赖 IntelliAI Engine 提供统一的 AI 能力入口。

## 功能概览

- ✅ 基于 Checkstyle-IDEA 扫描结果提供 AI QuickFix
- ✅ 使用“片段替换”策略，确保修复范围可控
- 🚧 后续将支持解析 Maven 插件生成的 Checkstyle/PMD XML，并在 Problem 面板中一键修复

## 依赖

- IntelliAI Engine 插件
- CheckStyle-IDEA 插件（可选依赖）

## 本地开发

```bash
./gradlew runIde
```

## 目录结构（核心）

- `src/main/java/dev/dong4j/zeka/stack/idea/plugin/repairer/checkstyle/`  
  Checkstyle AI 修复实现

## Roadmap

- [ ] 解析 Maven Checkstyle/PMD XML
- [ ] Problem 面板批量修复
- [ ] 修复结果预览与回滚
    - `copyAiCommonPlugin` → 复制到沙盒环境
    - `runIde` → 启动带完整依赖的 IDE

#### 4.1.3 生产发布流程

1. 确保 `plugin.xml` 中已声明 `<depends>`
2. 执行发布流程
3. 用户在 Marketplace 安装时，系统会自动提示安装 IntelliAI Engine 依赖

### 4.2 使用 AI 服务

```java
AIService aiService = ApplicationManager.getApplication().getService(AIService.class);
AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);
String result = aiService.generateContent(project, request, providerConfig, null);
```

## 5 📁 项目结构

```
template-with-ai/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/dong4j/zeka/stack/idea/plugin/example/
│   │   │       ├── action/
│   │   │       ├── PluginContents.java
│   │   │       ├── settings/
│   │   │       ├── statusbar/
│   │   │       ├── icons/
│   │   │       └── util/
│   │   └── resources/
│   │       ├── dev/dong4j/zeka/stack/idea/plugin/example/icons/
│   │       ├── messages/
│   │       └── META-INF/
│   └── test/
├── includes/
└── site/
    └── docs/
```

## 6 📖 文档

- `site/docs/用户手册.md`
- `site/docs/插件开发指南.md`

## 7 ⚠️ 注意事项

1. **Engine 版本管理**：`engineVersion` 在 `gradle.properties` 中统一管理，避免版本不一致
2. **本地开发路径**：`buildAiCommonPlugin` 任务假设 `intelli-ai-engine` 位于 `../intelli-ai-engine`
3. **发布前检查**：确保 `plugin.xml` 中已正确声明 Engine 依赖
4. **线程安全**：遵循 IntelliJ Platform 的线程模型，UI 操作在 EDT，耗时操作在 BGT
5. **统一常量**：插件 ID/Name 统一维护在 `PluginContents`，避免散落引用
