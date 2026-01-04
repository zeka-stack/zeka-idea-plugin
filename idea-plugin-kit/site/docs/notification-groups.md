# IntelliJ Platform 通知组类型说明

## 概述

IntelliJ Platform 提供了多种通知组类型，用于在不同场景下向用户显示通知。本文档说明 `IntelliAI Engine` 插件中定义的 4 种通知组的区别和使用场景。

## 通知组配置

```xml
<!--suppress PluginXmlI18n -->
<notificationGroup displayType="BALLOON" id="IntelliAI Engine" isLogByDefault="false"/>
<!--suppress PluginXmlI18n -->
<notificationGroup displayType="BALLOON" id="IntelliAI Engine Log" isLogByDefault="true"/>
<!--suppress PluginXmlI18n -->
<notificationGroup displayType="STICKY_BALLOON" id="IntelliAI Engine Sticky" isLogByDefault="false"/>
<!--suppress PluginXmlI18n -->
<notificationGroup displayType="STICKY_BALLOON" id="IntelliAI Engine Sticky Log" isLogByDefault="true"/>
```

## 属性说明

### displayType 属性

- **`BALLOON`**：普通气球通知
    - 自动消失（几秒后自动关闭）
    - 用户也可以手动关闭
    - 适合临时信息提示

- **`STICKY_BALLOON`**：粘性气球通知
    - 不会自动消失
    - 必须由用户手动关闭
    - 适合重要信息提示

### isLogByDefault 属性

- **`isLogByDefault="false"`**：默认不记录到事件日志
    - 通知会显示，但不会写入日志
    - 适合一般性提示信息

- **`isLogByDefault="true"`**：默认记录到事件日志
    - 通知会显示，并且会写入日志
    - 可以在 IDE 的 Event Log 中查看历史记录
    - 适合需要追踪的信息

## 4 种通知组对比

| 通知组                           | 显示类型 | 是否记录日志 | 使用场景               |
|-------------------------------|------|--------|--------------------|
| `IntelliAI Engine`            | 普通气球 | 否      | 临时提示信息（如操作成功提示）    |
| `IntelliAI Engine Log`        | 普通气球 | 是      | 需要记录的临时信息（如操作完成记录） |
| `IntelliAI Engine Sticky`     | 粘性气球 | 否      | 重要提示信息（如错误、警告）     |
| `IntelliAI Engine Sticky Log` | 粘性气球 | 是      | 重要且需要记录的信息（如严重错误）  |

## 使用建议

### 1. `IntelliAI Engine`

- **适用场景**：一般成功提示、信息确认
- **示例**：操作成功、配置已保存

### 2. `IntelliAI Engine Log`

- **适用场景**：需要追踪的操作记录
- **示例**：任务完成、批量处理结果

### 3. `IntelliAI Engine Sticky`

- **适用场景**：重要警告、需要用户注意的错误
- **示例**：配置错误、功能不可用

### 4. `IntelliAI Engine Sticky Log`

- **适用场景**：严重错误、需要记录和追踪的问题
- **示例**：系统错误、关键操作失败

## 代码示例

```java
// 使用普通通知（不记录日志）
NotificationUtil.showInfo(project, "操作成功完成");

// 使用记录日志的通知
NotificationUtil.showInfoWithLog(project, "任务处理完成，共处理 10 个文件");

// 使用粘性通知（不记录日志）
NotificationUtil.showErrorSticky(project, "配置错误，请检查设置");

// 使用粘性通知（记录日志）
NotificationUtil.showErrorStickyWithLog(project, "系统错误：无法连接到服务器");
```

## 注意事项

1. **性能考虑**：普通气球通知会自动消失，不会占用用户注意力，适合频繁的操作反馈
2. **用户体验**：粘性通知会一直显示直到用户关闭，应谨慎使用，避免打扰用户
3. **日志记录**：需要追踪的信息应使用 `isLogByDefault="true"` 的通知组
4. **通知优先级**：粘性通知的优先级高于普通通知，会覆盖普通通知

## 相关资源

- [IntelliJ Platform SDK - Notifications](https://plugins.jetbrains.com/docs/intellij/notifications.html)
- [NotificationGroup 文档](https://plugins.jetbrains.com/docs/intellij/notifications.html#notification-groups)

