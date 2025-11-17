# 贡献指南

感谢您对 IntelliJ IntelliDoc Assistant 插件项目的关注！

## 🚀 快速开始

### 1. Fork 并克隆项目

```bash
git clone https://github.com/zeka-stack/zeka-idea-plugin.git
cd zeka-idea-plugin/intelli-ai-javadoc
```

### 2. 设置开发环境

确保已安装：

- JDK 17 或更高版本
- IntelliJ IDEA（推荐使用最新版本）

### 3. 构建项目

```bash
./gradlew build
```

### 4. 运行测试

```bash
./gradlew test
```

## 🧪 添加测试

### 单元测试规范

1. **测试文件位置**: `src/test/java/com/github/intellijjavadocai/`
2. **命名规范**: `XxxTest.java` (对应类名 + Test)
3. **测试方法命名**: `test<功能名称>_<测试场景>`

### 测试示例

```java
@DisplayName("功能描述")
class MyFeatureTest {
    
    @BeforeEach
    void setUp() {
        // 初始化代码
    }
    
    @Test
    @DisplayName("测试场景描述")
    void testFeature_withSpecificScenario() {
        // Given
        // 准备测试数据
        
        // When
        // 执行测试
        
        // Then
        // 验证结果
        assertThat(result).isEqualTo(expected);
    }
}
```

### 测试覆盖率要求

- 新功能必须包含单元测试
- 测试覆盖率应达到 80% 以上
- 关键业务逻辑应达到 100% 覆盖

## 📝 代码规范

### Java 代码风格

- 使用 4 个空格缩进
- 遵循 Google Java Style Guide
- 添加必要的 JavaDoc 注释

### 提交信息格式

```
<类型>: <简短描述>

<详细描述>

<相关 Issue>
```

类型包括：

- `feat`: 新功能
- `fix`: 错误修复
- `test`: 添加或修改测试
- `docs`: 文档更新
- `refactor`: 代码重构
- `style`: 代码格式调整
- `chore`: 构建或辅助工具的变动

示例：

```
feat: 添加批量生成文档功能

- 支持为整个目录生成文档
- 添加进度显示
- 优化错误处理

Closes #123
```

## 🔍 代码审查

所有的 Pull Request 都需要经过代码审查。审查关注点：

1. ✅ 代码质量和可读性
2. ✅ 测试覆盖率
3. ✅ 文档完整性
4. ✅ 性能影响
5. ✅ 安全性

## 🐛 报告问题

报告问题时，请包含：

1. 问题描述
2. 复现步骤
3. 预期行为
4. 实际行为
5. 环境信息（IDE 版本、插件版本等）
6. 相关日志或截图

## 📞 联系方式

- GitHub Issues: [提交问题](https://github.com/zeka-stack/zeka-idea-plugin/issues)
- Email: dong4j@gmail.com

## 📜 许可证

通过提交 Pull Request，您同意您的贡献将在与项目相同的许可证下发布。

---

再次感谢您的贡献！🎉

