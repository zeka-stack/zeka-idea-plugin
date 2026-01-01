# IntelliJ IDEA 插件模板

提供两种 IntelliJ IDEA 插件开发模板，帮助快速创建新的插件项目。

## 📦 包含模板

### [带 AI 能力的插件模板](./template-with-ai)

适用于需要集成 AI 服务的插件开发。

**核心特性：**

- 🤖 已集成 IntelliAI Engine 依赖
- 🔧 自动构建和安装 Engine 插件的任务
- 📦 预配置 AI 服务调用接口
- 🎨 AI 相关 UI 组件支持

**详细说明：** 请查看 [template-with-ai/README.md](./template-with-ai/README.md)

### [不带 AI 能力的插件模板](./template-without-ai)

适用于纯工具类插件开发，无需 AI 集成。

**核心特性：**

- ⚡ 轻量级，无额外依赖
- 🛠️ 完整的插件开发环境
- 📦 预配置常用依赖和工具
- 🎯 专注于业务逻辑开发

**详细说明：** 请查看 [template-without-ai/README.md](./template-without-ai/README.md)

## 🚀 快速开始

### 使用带 AI 能力的模板

```bash
# 复制模板
cp -r template-with-ai my-new-ai-plugin
cd my-new-ai-plugin

# 修改 gradle.properties 中的插件信息
vim gradle.properties

# 开始开发
./gradlew runIde
```

### 使用不带 AI 能力的模板

```bash
# 复制模板
cp -r template-without-ai my-new-plugin
cd my-new-plugin

# 修改 gradle.properties 中的插件信息
vim gradle.properties

# 开始开发
./gradlew runIde
```

## ✅ 规范

### 图标资源

- 图标文件应放置在 `src/main/resources/dev/dong4j/zeka/stack/idea/plugin/example/icons/` 目录下
- 图标加载统一通过 `ExampleIcons` 管理，路径与包名保持一致
- 图标路径示例：`/dev/dong4j/zeka/stack/idea/plugin/example/icons/example_16.svg`

## 📚 更多信息

- [主项目 README](../README.md) - 查看完整的插件开发指南
- [IntelliAI Engine 集成详解](../README.md#-ai-插件-engine-依赖集成详解) - AI 插件开发详细说明
