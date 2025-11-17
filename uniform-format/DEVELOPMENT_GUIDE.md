# IntelliJ IDEA 插件开发模板使用指南

## 概述

这个模板是基于现有的 IntelliAI JavaDoc 插件项目抽离出来的完整 IntelliJ IDEA 插件开发模板。它包含了插件开发所需的所有基础组件和最佳实践。

## 模板特点

### 🚀 完整的项目结构

- 标准的 Gradle 项目结构
- 完整的包组织（action、service、settings、util）
- 测试代码示例

### 🎯 多种 Action 类型

- **快捷键 Action**: `Ctrl+Shift+E` / `Cmd+Shift+E`
- **编辑器右键菜单**: 文件操作
- **项目视图右键菜单**: 批量操作
- **Intention Action**: `Option+Enter` / `Alt+Enter`

### ⚙️ 完整的设置系统

- 设置状态管理 (`SettingsState`)
- 设置配置界面 (`ExampleSettingsConfigurable`)
- 设置面板 UI (`ExampleSettingsPanel`)
- 配置持久化存储

### 🔧 服务层架构

- 项目级服务 (`ExampleService`)
- 使用 `@Service` 注解管理生命周期
- 示例业务逻辑处理

### 🌍 国际化支持

- 英文和中文资源文件
- 资源包工具类 (`ExampleBundle`)
- 参数化消息支持

### 🧪 测试框架

- JUnit 5 测试框架
- Mockito 模拟框架
- AssertJ 断言库
- 示例单元测试

## 快速开始

### 1. 使用模板创建新项目

```bash
# 复制模板到新目录
cp -r template/ my-awesome-plugin/
cd my-awesome-plugin/

# 修改项目名称
# 编辑 settings.gradle.kts
rootProject.name = "my-awesome-plugin"
```

### 2. 修改插件信息

编辑 `src/main/resources/META-INF/plugin.xml`：

```xml
<id>com.yourcompany.awesome.plugin</id>
<name>My Awesome Plugin</name>
<vendor email="your@email.com">Your Company</vendor>
```

### 3. 修改包名

将所有 `com.example.plugin` 替换为你的包名：

```bash
# 使用 IDE 的重构功能或搜索替换
find src/ -name "*.java" -exec sed -i 's/com\.example\.plugin/com.yourcompany.awesome.plugin/g' {} \;
```

### 4. 构建和运行

```bash
# 构建插件
./gradlew build

# 运行插件（启动带插件的 IDE）
./gradlew runIde

# 运行测试
./gradlew test
```

## 开发指南

### 添加新的 Action

1. **创建 Action 类**：

```java
public class MyNewAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 你的逻辑
    }
}
```

2. **在 plugin.xml 中注册**：

```xml
<action id="com.yourcompany.plugin.action.MyNewAction"
        class="com.yourcompany.plugin.action.MyNewAction">
    <add-to-group group-id="EditorPopupMenu" anchor="last"/>
</action>
```

3. **添加资源文件条目**：

```properties
action.my.new=My New Action
action.my.new.description=Description of my new action
```

### 添加新的 Intention Action

1. **创建 Intention Action 类**：

```java
public class MyIntentionAction implements IntentionAction, PriorityAction {
    @Override
    public @NotNull String getText() {
        return "My Intention Action";
    }
    
    @Override
    public @NotNull String getFamilyName() {
        return "My Plugin";
    }
    
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file != null && file.getName().endsWith(".java");
    }
    
    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        // 你的逻辑
    }
    
    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
```

2. **在 plugin.xml 中注册**：

```xml
<intentionAction>
    <language>JAVA</language>
    <className>com.yourcompany.plugin.action.MyIntentionAction</className>
    <category>My Plugin</category>
    <descriptionDirectoryName>MyIntention</descriptionDirectoryName>
</intentionAction>
```

3. **创建描述文件**：
   在 `src/main/resources/intentionDescriptions/MyIntention/description.html` 创建 HTML 描述文件。

### 添加新的设置

1. **在 SettingsState 中添加字段**：

```java
public String myNewSetting = "default value";
```

2. **在设置面板中添加 UI**：

```java
private final JBTextField myNewTextField = new JBTextField();
```

3. **更新相关方法**：

```java
public boolean isModified(SettingsState settings) {
    return !myNewTextField.getText().equals(settings.getMyNewSetting());
}
```

### 添加新的服务

1. **创建服务类**：

```java
@Service(Service.Level.PROJECT)
public final class MyNewService {
    // 服务逻辑
}
```

2. **在 plugin.xml 中注册**（如果需要）：

```xml
<applicationService serviceImplementation="com.yourcompany.plugin.service.MyNewService"/>
```

### 添加国际化支持

1. **添加资源文件条目**：

```properties
# messages.properties
my.new.message=My new message: {0}

# messages_zh_CN.properties  
my.new.message=我的新消息: {0}
```

2. **使用资源包**：

```java
String message = ExampleBundle.message("my.new.message", "parameter");
```

## 配置说明

### Gradle 配置

模板使用最新的 IntelliJ Platform Gradle Plugin 2.x，支持：

- Java 17
- IntelliJ IDEA 2022.3+
- 自动测试框架配置
- 代码格式化工具

### 插件配置

- **兼容性**: IntelliJ IDEA 2022.3 及更高版本
- **依赖**: Java 模块
- **功能**: 通知系统、设置面板、多种 Action 类型

## 发布插件

### 1. 准备发布

1. **更新版本号**：

```kotlin
// build.gradle.kts
version = "1.0.0"
```

2. **配置签名**（可选）：

```bash
export CERTIFICATE_CHAIN="your-certificate-chain"
export PRIVATE_KEY="your-private-key"
export PRIVATE_KEY_PASSWORD="your-password"
```

### 2. 发布到 JetBrains Marketplace

1. **获取发布令牌**：
    - 访问 [JetBrains Marketplace](https://plugins.jetbrains.com/)
    - 创建插件并获取发布令牌

2. **设置环境变量**：

```bash
export PUBLISH_TOKEN="your-publish-token"
```

3. **发布插件**：

```bash
./gradlew publishPlugin
```

## 最佳实践

### 1. 代码组织

- 按功能模块组织包结构
- 使用服务层管理业务逻辑
- 保持 Action 类简洁，将复杂逻辑委托给服务

### 2. 错误处理

- 使用 `NotificationUtil` 显示用户友好的错误消息
- 在 Action 中检查必要的条件（项目、文件等）
- 使用 `update()` 方法控制 Action 的可用性

### 3. 性能优化

- 避免在 EDT 线程中执行耗时操作
- 使用后台任务处理长时间运行的操作
- 合理使用缓存和懒加载

### 4. 测试

- 为服务类编写单元测试
- 使用 Mockito 模拟 IntelliJ Platform 对象
- 测试边界条件和错误情况

## 常见问题

### Q: 插件在 IDE 中不显示？

A: 检查 `plugin.xml` 中的 `id` 是否唯一，确保没有与其他插件冲突。

### Q: Action 不响应？

A: 检查 `plugin.xml` 中的 Action 注册，确保类名和包名正确。

### Q: 设置不保存？

A: 确保 `SettingsState` 正确实现了 `PersistentStateComponent` 接口。

### Q: 国际化不生效？

A: 检查资源文件路径和 `plugin.xml` 中的 `resource-bundle` 配置。

## 参考资源

- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Gradle Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
- [Plugin Development Guide](https://plugins.jetbrains.com/docs/intellij/plugin-development.html)
- [IntelliJ Platform Explorer](https://jb.gg/ipe)

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个模板！

## 许可证

MIT License - 详见 LICENSE 文件。
