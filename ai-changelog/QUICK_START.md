# IntelliJ IDEA 插件开发模板

基于 AI JavaDoc 插件项目抽离的完整 IntelliJ IDEA 插件开发模板。

## 功能特性

- ✅ 完整的 Gradle 项目结构
- ✅ 多种 Action 类型（快捷键、右键菜单、Intention Action）
- ✅ 完整的设置系统（配置界面、持久化存储）
- ✅ 服务层架构（项目级服务管理）
- ✅ 国际化支持（中英文资源文件）
- ✅ 测试框架（JUnit 5 + Mockito + AssertJ）
- ✅ 通知系统
- ✅ 工具类库

## 快速开始

1. **复制模板**：

```bash
cp -r template/ your-plugin-name/
cd your-plugin-name/
```

2. **修改配置**：
    - 编辑 `settings.gradle.kts` 修改项目名
    - 编辑 `build.gradle.kts` 修改 group 和 version
    - 编辑 `plugin.xml` 修改插件信息
    - 替换包名 `com.example.plugin` 为你的包名

3. **构建运行**：

```bash
./gradlew build
./gradlew runIde
./gradlew test
```

## 项目结构

```
template/
├── build.gradle.kts              # Gradle 构建配置
├── settings.gradle.kts            # Gradle 设置
├── gradlew                       # Gradle Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/example/plugin/
│   │   │   ├── action/           # Action 类
│   │   │   ├── service/          # 服务类
│   │   │   ├── settings/         # 设置相关
│   │   │   └── util/             # 工具类
│   │   └── resources/
│   │       ├── META-INF/plugin.xml
│   │       ├── messages.properties
│   │       └── messages_zh_CN.properties
│   └── test/                     # 测试代码
├── README.md                     # 使用说明
└── DEVELOPMENT_GUIDE.md          # 开发指南
```

## Action 类型

- **快捷键**: `Ctrl+Shift+E` / `Cmd+Shift+E`
- **编辑器右键**: 文件操作
- **项目视图右键**: 批量操作
- **Intention Action**: `Option+Enter` / `Alt+Enter`

## 设置系统

- 设置面板：`Settings → Tools → Example Plugin`
- 配置持久化存储
- 支持基础设置和高级设置

## 开发指南

详见 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)

## 参考资源

- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Gradle Plugin](https://github.com/JetBrains/gradle-intellij-plugin)

## 许可证

MIT License
