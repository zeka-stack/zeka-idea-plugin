# Java 测试项目

这是一个简单的 Java 项目，用于测试 IntelliAI Javadoc 插件的 Java 支持功能。

## 项目结构

```
test-java-project/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── example/
│   │               ├── UserService.java      # 服务类
│   │               ├── User.java             # 实体类
│   │               ├── UserRepository.java   # 仓库类
│   │               ├── UserController.java  # 控制器类
│   │               ├── Calculator.java       # 工具类
│   │               └── Status.java           # 枚举类
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── UserServiceTest.java  # 测试类
├── pom.xml
└── README.md
```

## 测试内容

这个项目包含了以下 Java 元素，用于测试 Javadoc 生成：

1. **类 (Class)**: `UserService`, `UserRepository`, `UserController`
2. **实体类 (Entity Class)**: `User`
3. **工具类 (Utility Class)**: `Calculator`
4. **枚举 (Enum)**: `Status`
5. **方法 (Method)**: 各种公共和私有方法
6. **字段 (Field)**: 各种属性和成员变量
7. **测试方法**: `UserServiceTest` 中的测试方法（带 `@Test` 注解）

## 使用方法

1. 在 IntelliJ IDEA 中打开这个项目
2. 确保已安装并启用 IntelliAI Javadoc 插件
3. 在设置中启用 Java 语言支持（默认已启用）
4. 使用以下方式测试 Javadoc 生成：
    - 将光标放在类、方法或字段上，按 `Alt+Enter` 选择生成文档
    - 或使用快捷键 `Ctrl+Shift+D` (Windows/Linux) 或 `Cmd+Shift+D` (Mac)
   - 或在右键菜单中选择"Generate Javadoc"
   - 或在 Generate 菜单（`Alt+Insert` 或 `Cmd+N`）中选择生成 Javadoc

## 测试场景

### 1. 类级别文档生成

- 将光标放在类名上（如 `UserService`），生成类的 Javadoc

### 2. 方法级别文档生成

- 将光标放在方法上（如 `findUserById`），生成方法的 Javadoc
- 测试带参数的方法
- 测试带返回值的方法
- 测试可能抛出异常的方法

### 3. 字段级别文档生成

- 将光标放在字段上（如 `currentUser`），生成字段的 Javadoc

### 4. 测试方法文档生成

- 将光标放在测试方法上（如 `testFindUserById_whenUserExists_shouldReturnUser`），生成测试方法的 Javadoc

### 5. 批量生成

- 选中整个文件，为文件中的所有元素生成文档
- 选中整个目录，为目录中所有文件生成文档

## 注意事项

- 确保插件已正确安装和启用
- 在插件设置中检查 Java 语言支持是否已启用
- 生成的文档格式为 Javadoc（使用 `/** */` 格式）
- 如果遇到问题，检查插件设置中的 AI 提供商配置是否正确

## Maven 构建

如果使用 Maven 构建项目：

```bash
# 编译项目
mvn compile

# 运行测试
mvn test

# 生成 Javadoc（使用 Maven Javadoc 插件）
mvn javadoc:javadoc
```

## 项目特点

- 使用 Java 17
- 使用 JUnit 5 进行单元测试
- 包含常见的代码模式（服务层、仓库层、控制器层）
- 包含各种类型的代码元素（类、方法、字段、枚举、测试）

