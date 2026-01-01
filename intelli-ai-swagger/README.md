# IntelliAI Swagger

这是一个基于 IntelliAI Engine 的 IntelliJ IDEA 插件，用于生成和维护 Swagger/OpenAPI 注解与文档。

## 功能概览

- Controller 方法级 Swagger 注解生成
- DTO/Schema 注解补全与更新
- AI 服务商选择与统一配置复用
- 状态栏快捷入口与右键 Action 示例

## 开发与调试

```bash
# 构建插件
./gradlew build

# 启动带插件的 IDE
./gradlew runIde

# 运行测试
./gradlew test
```

## 依赖说明

本插件依赖 IntelliAI Engine（运行时通过 plugin.xml 声明），本地开发时会自动构建并复制到沙盒。
