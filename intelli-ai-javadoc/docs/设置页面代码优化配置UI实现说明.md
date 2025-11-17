# 设置页面代码优化配置 UI 实现说明

## 功能概述

为了使用户能够自定义代码优化功能，我们在设置页面添加了两个新的配置选项：

1. **优化类代码以减少 token 消耗**：控制是否启用代码优化功能
2. **类代码最大行数**：控制代码截取的行数限制

## UI 实现

### 1. 新增 UI 组件

**文件**: `src/main/java/com/github/intellijjavadocai/settings/ui/JavaDocSettingsPanel.java`

#### 组件声明

```java
// 功能配置
private JBCheckBox optimizeClassCodeCheckBox;
private JSpinner maxClassCodeLinesSpinner;
```

#### 组件初始化

```java
// 功能配置
optimizeClassCodeCheckBox = new JBCheckBox(JavaDocBundle.message("settings.optimize.class.code"));
maxClassCodeLinesSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 5000, 100));
```

### 2. UI 布局

#### 主面板构建

```java
.addComponent(new JBLabel(JavaDocBundle.message("settings.generation.options")))
.addComponent(generateForClassCheckBox)
.addComponent(generateForMethodCheckBox)
.addComponent(generateForFieldCheckBox)
.addComponent(skipExistingCheckBox)
.addComponent(optimizeClassCodeCheckBox)  // 新增
.addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.max.class.code.lines")), maxClassCodeLinesSpinner)  // 新增
.addSeparator(10)
```

### 3. 事件监听

#### 智能启用/禁用

```java
// 监听代码优化配置变更
optimizeClassCodeCheckBox.addActionListener(e -> {
    // 当启用/禁用代码优化时，可以更新最大行数输入框的可用性
    maxClassCodeLinesSpinner.setEnabled(optimizeClassCodeCheckBox.isSelected());
});
```

### 4. 配置管理

#### 获取配置

```java
// 功能配置
settings.optimizeClassCode = optimizeClassCodeCheckBox.isSelected();
settings.maxClassCodeLines = (Integer) maxClassCodeLinesSpinner.getValue();
```

#### 加载配置

```java
// 功能配置
optimizeClassCodeCheckBox.setSelected(settings.optimizeClassCode);
maxClassCodeLinesSpinner.setValue(settings.maxClassCodeLines);

// 根据代码优化设置更新最大行数输入框的可用性
maxClassCodeLinesSpinner.setEnabled(settings.optimizeClassCode);
```

## 国际化支持

### 1. 英文消息

**文件**: `src/main/resources/messages.properties`

```properties
settings.optimize.class.code=Optimize class code to reduce token usage
settings.max.class.code.lines=Max class code lines:
```

### 2. 中文消息

**文件**: `src/main/resources/messages_zh_CN.properties`

```properties
settings.optimize.class.code=优化类代码以减少 token 消耗
settings.max.class.code.lines=类代码最大行数：
```

## 用户体验设计

### 1. 智能交互

- **关联控制**：当禁用代码优化时，最大行数输入框自动禁用
- **实时反馈**：用户修改配置时立即生效
- **默认值**：提供合理的默认配置

### 2. 配置范围

- **代码优化开关**：100-5000 行范围，默认 1000 行
- **步进值**：100 行步进，便于用户调整
- **边界保护**：防止用户输入不合理的值

### 3. 视觉设计

- **分组布局**：放在"生成选项"分组中，逻辑清晰
- **标签说明**：清晰的标签和提示信息
- **一致性**：与其他配置项保持一致的样式

## 功能特性

### 1. 代码优化开关

- **默认启用**：默认情况下优化功能是开启的
- **用户控制**：用户可以根据需要启用或禁用
- **即时生效**：配置修改后立即生效

### 2. 行数限制配置

- **可调节范围**：100-5000 行，满足不同项目需求
- **智能步进**：100 行步进，便于精确控制
- **关联控制**：只有在启用优化时才可配置

### 3. 配置持久化

- **自动保存**：配置修改后自动保存到设置文件
- **默认恢复**：重置设置时恢复默认值
- **跨会话保持**：重启 IDE 后配置保持不变

## 使用场景

### 1. 大型项目

- **大型类**：对于包含大量代码的类，可以设置较大的行数限制
- **性能优先**：启用优化以减少 token 消耗，提升性能

### 2. 小型项目

- **完整保留**：对于小型类，可以禁用优化以保留完整代码
- **精确控制**：设置较小的行数限制以精确控制

### 3. 调试场景

- **临时禁用**：调试时可以临时禁用优化功能
- **灵活调整**：根据测试结果灵活调整配置

## 技术细节

### 1. 组件类型选择

- **JBCheckBox**：用于开关控制，符合 IntelliJ Platform 设计规范
- **JSpinner**：用于数值输入，提供范围限制和步进控制

### 2. 事件处理

- **ActionListener**：处理复选框状态变更
- **实时更新**：状态变更时立即更新相关组件

### 3. 数据绑定

- **双向绑定**：UI 组件与配置对象双向绑定
- **类型安全**：使用强类型确保数据正确性

## 测试验证

### 1. UI 测试

- ✅ 组件正确显示
- ✅ 事件监听正常工作
- ✅ 配置保存和加载正确
- ✅ 国际化消息正确显示

### 2. 功能测试

- ✅ 代码优化开关正常工作
- ✅ 行数限制配置正确应用
- ✅ 关联控制逻辑正确
- ✅ 默认值设置正确

### 3. 边界测试

- ✅ 最小值边界处理
- ✅ 最大值边界处理
- ✅ 禁用状态处理
- ✅ 配置重置处理

## 注意事项

### 1. 兼容性

- **向后兼容**：不影响现有配置
- **默认行为**：保持原有的默认行为
- **渐进增强**：新功能是可选的

### 2. 性能考虑

- **UI 响应**：配置变更不会影响 UI 响应性能
- **内存使用**：组件创建和销毁不会造成内存泄漏
- **事件处理**：事件监听器不会造成性能问题

### 3. 用户体验

- **直观操作**：配置选项直观易懂
- **即时反馈**：配置变更立即生效
- **错误处理**：提供合理的错误提示

