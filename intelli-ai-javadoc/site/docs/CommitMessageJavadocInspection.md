# dev.dong4j.zeka.stack.idea.javadoc.git.CommitMessageJavadocInspection

## 作用范围

该实现属于 **Commit Message Inspections（提交消息检查）**，不是 Commit Checks。它只在提交消息编辑器内运行，用于提示提交内容中可能存在的 Javadoc
缺失问题。

## 何时生效

- 打开提交面板、提交消息输入框初始化时触发一次检查
- 提交消息编辑器内容变更时，IDEA 会根据 inspection 机制重新触发检查

因此它不会在点击提交按钮时执行，也不会阻止提交，仅在提交消息区域显示告警提示。

## 出现在 IDEA 的哪个位置

位置：`Settings | Version Control | Commit | 提交消息检查`

该项作为“提交消息检查”的一条规则出现，用户可勾选启用/禁用。

## 技术接入点

1. **实现**：继承 `com.intellij.vcs.commit.message.BaseCommitMessageInspection`
2. **注册**：在启动后将 inspection 动态加入 `CommitMessageInspectionProfile`
3. **变更文件范围**：通过 `CommitMessage.CHANGES_SUPPLIER_KEY` 获取当前提交面板中用户勾选的变更文件

核心流程：

```java
// 获取提交消息编辑器绑定的变更文件集合（仅勾选项）
Supplier<Iterable<Change>> changesSupplier = document.getUserData(CommitMessage.CHANGES_SUPPLIER_KEY);
```

## 行为特征

- 触发时机与“提交消息编辑器”生命周期绑定
- 只做警告，不阻断提交
- 不适合作为“点击提交按钮后才执行”的检查

## 适用场景

- 需要在用户编辑提交消息时实时提示的规则
- 对提交内容的校验不需要在“提交按钮点击”时同步阻塞

## 不适用场景

- 必须在点击提交按钮后执行的检查
- 需要基于明确的“提交动作”给出一次性提醒
