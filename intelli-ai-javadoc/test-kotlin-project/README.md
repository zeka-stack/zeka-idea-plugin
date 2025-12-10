# Kotlin 测试项目

这是一个简单的 Kotlin 项目，用于测试 IntelliAI Javadoc 插件的 Kotlin 支持功能。

## 项目结构

```
test-kotlin-project/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/
│   │           └── example/
│   │               ├── UserService.kt      # 服务类
│   │               ├── User.kt             # 数据类
│   │               ├── UserRepository.kt   # 仓库类
│   │               ├── Calculator.kt        # 工具对象
│   │               └── Status.kt          # 枚举类
│   └── test/
│       └── kotlin/
│           └── com/
│               └── example/
│                   └── UserServiceTest.kt  # 测试类
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 测试内容

这个项目包含了以下 Kotlin 元素，用于测试 KDoc 生成：

1. **类 (Class)**: `UserService`, `UserRepository`
2. **数据类 (Data Class)**: `User`
3. **对象 (Object)**: `Calculator`
4. **枚举 (Enum)**: `Status`
5. **函数 (Function)**: 各种公共和私有函数
6. **属性 (Property)**: 各种属性和字段
7. **测试函数**: `UserServiceTest` 中的测试方法

## 使用方法

1. 在 IntelliJ IDEA 中打开这个项目
2. 确保已安装并启用 IntelliAI Javadoc 插件
3. 在设置中启用 Kotlin 语言支持
4. 使用以下方式测试 KDoc 生成：
    - 将光标放在类、函数或属性上，按 `Alt+Enter` 选择生成文档
    - 或使用快捷键 `Ctrl+Shift+D` (Windows/Linux) 或 `Cmd+Shift+D` (Mac)
   - 或在右键菜单中选择"Generate Javadoc"

## 注意事项

- 确保插件已更新到支持 Kotlin 的版本
- 在插件设置中启用 Kotlin 语言支持
- 生成的文档格式为 KDoc（Kotlin 文档注释格式）

