# Commit Message Inlay Hint 实现方案

## 一、背景

### 1.1 问题场景

IntelliAI Changelog 插件支持将用户在提交消息输入框中输入的内容作为上下文提供给 AI，用于生成更准确的提交信息。该功能通过
`SettingsState.useCommitMessageInputAsContext` 设置开关控制。

#### 1.1.1 现有问题

当用户启用了 `useCommitMessageInputAsContext` 设置后，存在以下问题：

1. **用户遗忘风险**：用户可能忘记已经开启了此功能
    - 在多次提交过程中，开关状态是持久化的
    - 用户可能在不知情的情况下，将之前残留的提交信息作为新提交的上下文

2. **上下文污染问题**：如果提交消息输入框中存在之前未清空的内容
    - 这些内容会被误当作当前提交的上下文
    - 导致 AI 生成的提交信息不准确，与当前代码变更不匹配
    - 例如：上次提交关于 "修复登录bug" 的内容残留，本次要提交 "优化缓存逻辑"，但 AI 可能会混合两个上下文

3. **用户体验问题**：为了提醒用户此功能已启用，当前实现会在提交前显示一个 Tip 提示气泡
    - 但这种方式仍然需要用户中断操作去查看提示
    - 提示可能被用户忽略或误解
    - 无法提供流畅的交互体验

#### 1.1.2 交互流程痛点

用户生成提交信息的典型流程：

1. 用户编写代码并准备提交
2. 打开 Git Commit 对话框
3. **如果启用了 `useCommitMessageInputAsContext`**：
    - 用户在输入框中输入自然语言描述（如 "优化缓存逻辑"）
    - 点击工具栏按钮或使用快捷键触发生成
    - 系统读取输入框内容 + diff → 生成提交信息

**痛点**：

- 用户需要**主动点击按钮或使用快捷键**才能触发生成
- 这个操作**打断了用户输入提交信息的思维流**
- 用户在输入过程中需要停下来去触发功能，体验不够流畅

### 1.2 解决方案目标

为了进一步优化交互体验，我们引入 **Inlay Hint + Tab 触发**的方式：

1. **非侵入式提示**：当用户在 Commit Message 编辑器中输入文本时，自动在光标后显示 Inlay 提示
    - 提示内容为"⇥ 生成提交信息"（Tab 键图标 + 操作说明）
    - 提示不影响文档内容，仅作为视觉引导
    - 类似于代码补全的交互体验

2. **无缝触发**：用户按 Tab 键即可触发 Changelog 生成功能
    - 避免打断用户输入提交信息的思维流
    - 保持输入连续性，提升操作效率
    - 符合用户对 Tab 键的认知（通常用于补全或触发建议）

3. **智能控制**：仅在启用 `useCommitMessageInputAsContext` 时显示提示
    - 确保提示功能的上下文和意义
    - 避免在不相关场景下显示误导性提示

### 1.3 前置条件

Inlay Hint 功能依赖于以下设置：

- **必须启用**：`SettingsState.useCommitMessageInputAsContext = true`
    - 此设置控制是否将用户在提交消息输入框中输入的内容作为上下文提供给 AI
    - 当该设置为 `false` 时，Inlay Hint 将不会显示
    - 用户可以在设置页面（Settings → Tools → IntelliAI Changelog）或状态栏切换此设置
    - 该功能的目的就是优化此开关启用后的交互体验

### 1.4 参考实现

该方案参考了以下 IntelliJ 平台原生功能：

- **GitHub Copilot 插件**：使用 Inline Inlay 显示代码建议
- **AI Assistant**：在编辑器中显示智能提示
- **代码补全**：Tab 键触发补全的交互模式

## 二、需求分析

### 2.1 功能需求

1. **提示显示条件**
    - 光标位于 Commit Message 编辑器中
    - 当前内容非空（至少有文本输入）
    - 没有选中文本（光标为插入模式）
    - 插件功能已启用
    - **`SettingsState.useCommitMessageInputAsContext = true`**（必须启用"将提交面板输入的说明作为上下文"设置）
    - 当前没有正在进行的生成任务

   > **说明**：Inlay Hint 功能是为了优化 `useCommitMessageInputAsContext` 开关启用后的交互体验。当用户启用此功能后，可能忘记已经开启，或者不知道如何触发生成。Inlay
   Hint 通过非侵入式的视觉提示，让用户在输入自然语言描述后，可以无缝地按 Tab 键触发生成，避免打断输入思维流。只有当用户启用了"
   使用提交消息输入作为上下文"功能时，显示此提示才有意义。如果该设置未启用，提示将不会显示。

2. **提示内容**
    - 显示 Tab 键图标或快捷键文本
    - 显示操作说明文本（如"生成提交信息"）
    - 使用灰色/半透明样式，与真实文本区分

3. **交互触发**
    - 用户按 Tab 键时，检查是否有提示显示
    - 如果有提示，触发 Changelog 生成功能
    - 如果没有提示，Tab 键执行原有功能（缩进等）

4. **生命周期管理**
    - 用户开始输入时显示提示
    - 用户移动光标时更新提示位置
    - 用户删除文本时自动隐藏提示
    - 编辑器失去焦点时清理提示

### 2.2 技术约束

1. **编辑器类型**
    - Commit Message 使用 `EditorTextField`，底层是 IntelliJ `Editor`
    - 不能使用 Swing 层的 `KeyListener`，需要走 IntelliJ 平台体系

2. **线程安全**
    - UI 更新必须在 EDT（Event Dispatch Thread）中执行
    - 提示显示/隐藏需要与编辑器的生命周期绑定

3. **性能要求**
    - 提示显示/隐藏不应影响编辑器性能
    - 避免频繁创建/销毁 Inlay 对象

## 三、技术方案

### 3.1 架构设计

```
CommitMessageEditorHintManager (管理器)
├─ EditorCaretListener (监听器)
│  └─ 监听光标位置变化，决定是否显示提示
├─ InlayHintService (提示服务)
│  ├─ showHint() - 显示提示
│  ├─ hideHint() - 隐藏提示
│  └─ updateHint() - 更新提示位置
├─ GenerateCommitAction (Tab 键 Action)
│  ├─ 检查是否有提示显示
│  ├─ 收集 diff + 用户输入
│  └─ 调用 AI 生成服务
└─ State (状态管理)
   ├─ 是否显示 hint
   ├─ 是否生成中
   └─ 编辑器引用
```

### 3.2 核心组件

#### 3.2.1 Inline Inlay 提示

使用 IntelliJ 平台的 `InlayModel` API 在光标后插入内联提示：

**优点**：

- 不进入文档内容，不影响 `getText()`
- 自动跟随光标移动
- 可随时移除，不影响文本编辑
- 与编辑器样式系统集成（支持主题切换）

**实现示例**：

```java
InlayModel inlayModel = editor.getInlayModel();
InlayProperties properties = new InlayProperties();
properties.relatesToPrecedingText(true);  // 与前文关联
properties.disableSoftWrapping(true);     // 禁用软换行

Inlay<?> inlay = inlayModel.addInlineElement(
    cursorOffset,  // 光标位置
    properties,
    new CommitMessageHintRenderer(editor)
);
```

#### 3.2.2 自定义渲染器

实现 `EditorCustomElementRenderer` 接口，自定义提示的显示样式：

**显示内容**：

- Tab 键图标或快捷键文本（如 "Tab"）
- 操作说明文本（如 "生成提交信息"）
- 可选的视觉分隔符

**样式设计**：

- 背景：半透明灰色矩形（带圆角）
- 文本：灰色，与编辑器前景色协调
- 边框：浅色边框，区分提示区域
- 字体：比编辑器字体稍小

#### 3.2.3 Tab 键拦截

使用 IntelliJ Action 系统，而非 Swing KeyListener：

**实现步骤**：

1. **定义 Action**：
   ```xml
   <action id="Changelog.GenerateFromHint"
           class="dev.dong4j.changelog.action.GenerateCommitFromHintAction"
           text="Generate Commit Message from Hint"/>
   ```

2. **绑定快捷键**（仅针对 Commit Message Editor）：
   ```java
   @Override
   public void actionPerformed(@NotNull AnActionEvent e) {
       Editor editor = e.getData(CommonDataKeys.EDITOR);
       if (!isCommitMessageEditor(editor)) {
           e.getPresentation().setEnabled(false);
           return;  // 让 Tab 键继续执行原有功能
       }

       if (!hasActiveHint(editor)) {
           e.getPresentation().setEnabled(false);
           return;  // 没有提示时不拦截
       }

       // 触发生成
       generateCommitMessage(editor);
   }
   ```

3. **条件判断**：
    - 检查编辑器是否为 Commit Message Editor
    - 检查是否有活跃的 Inlay 提示
    - 检查是否在补全状态下（避免冲突）

#### 3.2.4 编辑器识别

准确识别 Commit Message Editor 是关键，需要区分：

- ✅ Commit Message Editor（VCS Commit 对话框）
- ❌ 普通代码编辑器
- ❌ 其他文本编辑器（如 Issue 描述）

**识别方法**：

```java
private boolean isCommitMessageEditor(@NotNull Editor editor) {
    // 方法1：检查 Editor 的上下文
    DataContext dataContext = DataManager.getInstance()
        .getDataContext(editor.getContentComponent());
    CommitMessageI commitMessage = dataContext.getData(COMMIT_MESSAGE);
    if (commitMessage != null) {
        return true;
    }

    // 方法2：检查父组件类型
    Component parent = SwingUtilities.getAncestorOfClass(
        CommitMessagePanel.class,
        editor.getContentComponent()
    );
    return parent != null;

    // 方法3：检查文件类型或虚拟文件
    VirtualFile file = FileDocumentManager.getInstance()
        .getFile(editor.getDocument());
    // ... 根据文件路径或类型判断
}
```

#### 3.2.5 生命周期管理

使用 `Disposable` 模式管理资源：

```java
public class CommitMessageHintManager implements Disposable {
    private final Editor editor;
    private Inlay<?> currentInlay;
    private CaretListener caretListener;

    public CommitMessageHintManager(@NotNull Editor editor,
                                    @NotNull Disposable parent) {
        this.editor = editor;
        Disposer.register(parent, this);
        setupListeners();
    }

    private void setupListeners() {
        // 监听光标移动，更新提示位置
        caretListener = e -> updateHint();
        editor.getCaretModel().addCaretListener(caretListener);

        // 监听文档内容变化，更新提示显示状态
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                updateHint();
            }
        }, this);

        // 注意：SettingsState 的改变需要通过其他方式监听
        // 例如在 updateHint() 中检查设置状态，或者在设置变更时主动调用更新
    }

    @Override
    public void dispose() {
        if (currentInlay != null) {
            currentInlay.dispose();
            currentInlay = null;
        }
        if (caretListener != null) {
            editor.getCaretModel().removeCaretListener(caretListener);
        }
    }
}
```

## 四、实现细节

### 4.1 提示显示逻辑

```java
private void updateHint() {
    if (!shouldShowHint()) {
        hideHint();
        return;
    }

    int offset = editor.getCaretModel().getOffset();
    if (currentInlay != null) {
        // 检查位置是否变化
        int inlayOffset = currentInlay.getOffset();
        if (inlayOffset != offset) {
            hideHint();
            showHint(offset);
        }
    } else {
        showHint(offset);
    }
}

private boolean shouldShowHint() {
    // 检查条件
    if (!isCommitMessageEditor(editor)) return false;
    if (editor.getDocument().getTextLength() == 0) return false;
    if (editor.getSelectionModel().hasSelection()) return false;
    if (isGenerating()) return false;
    if (!isPluginEnabled()) return false;
    // 检查是否启用了"使用提交消息输入作为上下文"设置
    if (!SettingsState.getInstance().useCommitMessageInputAsContext) return false;
    return true;
}
```

### 4.2 提示渲染实现

```java
public class CommitMessageHintRenderer implements EditorCustomElementRenderer {
    private static final String HINT_TEXT = "生成提交信息";
    private final Editor editor;

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        FontMetrics fm = getFontMetrics();
        String tabText = getTabShortcutText();
        int tabWidth = fm.stringWidth(tabText);
        int textWidth = fm.stringWidth(HINT_TEXT);
        int padding = JBUI.scale(16);
        int spacing = JBUI.scale(8);
        return tabWidth + textWidth + padding * 2 + spacing;
    }

    @Override
    public void paint(@NotNull Inlay inlay,
                      @NotNull Graphics g,
                      @NotNull Rectangle targetRegion,
                      @NotNull TextAttributes textAttributes) {
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            // 绘制背景
            Color bgColor = withAlpha(
                editor.getColorsScheme().getDefaultBackground(),
                0.85f
            );
            g2d.setColor(bgColor);
            g2d.fillRoundRect(
                targetRegion.x, targetRegion.y,
                targetRegion.width, targetRegion.height,
                6, 6
            );

            // 绘制文本
            Font font = getHintFont();
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();

            Color textColor = withAlpha(
                editor.getColorsScheme().getDefaultForeground(),
                0.7f
            );
            g2d.setColor(textColor);

            String tabText = getTabShortcutText();
            int x = targetRegion.x + JBUI.scale(8);
            int y = targetRegion.y + (targetRegion.height + fm.getAscent()) / 2;

            g2d.drawString(tabText, x, y);
            x += fm.stringWidth(tabText) + JBUI.scale(4);
            g2d.drawString(HINT_TEXT, x, y);
        } finally {
            g2d.dispose();
        }
    }
}
```

### 4.3 Tab 键处理

```java
public class GenerateCommitFromHintAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();

        if (editor == null || project == null) {
            return;
        }

        // 检查是否为 Commit Message Editor
        if (!CommitMessageHintManager.isCommitMessageEditor(editor)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查是否启用了"使用提交消息输入作为上下文"设置
        if (!SettingsState.getInstance().useCommitMessageInputAsContext) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查是否有活跃提示
        CommitMessageHintManager hintManager =
            CommitMessageHintManager.getInstance(editor);
        if (!hintManager.hasActiveHint()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查是否在补全状态
        if (LookupManager.getInstance(project).getActiveLookup() != null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 触发生成
        String userInput = editor.getDocument().getText();
        Collection<Change> changes = getCurrentChanges(project);

        CommitMessageGenerator generator =
            CommitMessageGenerator.getInstance(project);
        generator.generateForChanges(changes, getCommitMessageControl(editor), null);

        // 隐藏提示
        hintManager.hideHint();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 动态启用/禁用
        boolean enabled = CommitMessageHintManager.isCommitMessageEditor(editor) &&
                         SettingsState.getInstance().useCommitMessageInputAsContext &&
                         CommitMessageHintManager.getInstance(editor).hasActiveHint();
        e.getPresentation().setEnabled(enabled);
    }
}
```

## 五、配置与扩展

### 5.1 插件配置

在 `plugin.xml` 中注册：

```xml
<actions>
    <action id="Changelog.GenerateFromHint"
            class="dev.dong4j.changelog.action.GenerateCommitFromHintAction"
            text="Generate Commit Message from Hint">
        <keyboard-shortcut keymap="$default" first-keystroke="TAB"/>
        <override-text place="MainMenu" text="Generate Commit Message"/>
    </action>
</actions>

<applicationService
    serviceImplementation="dev.dong4j.changelog.hint.CommitMessageHintService"/>
```

### 5.2 可配置项

在设置中提供以下选项：

1. **启用/禁用提示**：允许用户关闭此功能
2. **提示文本自定义**：允许用户修改提示文本
3. **显示延迟**：输入后延迟多久显示提示（避免频繁闪烁）
4. **快捷键自定义**：允许用户修改触发快捷键

### 5.3 国际化

提示文本支持多语言：

```properties
# ChangelogBundle.properties
commit.hint.tab.text=Tab
commit.hint.action.text=Generate commit message

# ChangelogBundle_zh_CN.properties
commit.hint.tab.text=Tab
commit.hint.action.text=生成提交信息
```

## 六、测试策略

### 6.1 单元测试

1. **提示显示逻辑测试**
    - 验证各种条件下提示的显示/隐藏
    - 验证光标移动时提示位置更新
    - **验证当 `useCommitMessageInputAsContext = false` 时提示不显示**
    - **验证当 `useCommitMessageInputAsContext = true` 时提示正常显示**
    - **验证设置变更时提示立即更新显示状态**

2. **编辑器识别测试**
    - 验证能正确识别 Commit Message Editor
    - 验证不会误识别其他编辑器

3. **Tab 键拦截测试**
    - 验证在有提示时正确拦截
    - 验证在没有提示时不拦截
    - **验证当 `useCommitMessageInputAsContext = false` 时 Tab 键不拦截**
    - **验证当 `useCommitMessageInputAsContext = true` 且有提示时 Tab 键正确拦截**

### 6.2 集成测试

1. **完整流程测试**
    - 输入文本 → 显示提示 → 按 Tab → 生成提交信息

2. **边界情况测试**
    - 快速输入/删除文本
    - 切换编辑器焦点
    - 在生成过程中操作

3. **性能测试**
    - 验证频繁输入时不会出现性能问题
    - 验证内存泄漏（Inlay 是否正确释放）

## 七、注意事项

### 7.1 兼容性

- **IntelliJ 版本**：确保 Inlay API 在目标版本中可用（2020.1+）
- **其他插件**：避免与其他使用 Tab 键的插件冲突
- **主题支持**：确保提示在不同主题下都清晰可见

### 7.2 用户体验

1. **不干扰原有功能**：Tab 键在非 Commit Message Editor 中保持原有行为
2. **清晰的可视反馈**：提示样式要明显但不刺眼
3. **快速响应**：提示显示/隐藏要流畅，不能有延迟感
4. **设置依赖**：Inlay Hint 功能仅在 `useCommitMessageInputAsContext = true` 时显示
    - 用户需要在设置页面或状态栏启用"将提交面板输入的说明作为上下文"功能
    - 当设置关闭时，提示将自动隐藏，避免用户困惑
    - 设置变更时应立即更新提示显示状态

### 7.3 性能优化

1. **延迟显示**：用户停止输入一段时间后再显示提示（避免频繁创建/销毁）
2. **防抖处理**：光标快速移动时不立即更新提示位置
3. **资源管理**：及时释放不再使用的 Inlay 对象

## 八、后续优化

### 8.1 功能增强

1. **智能提示**：根据输入内容智能判断是否需要提示
2. **多语言支持**：提示文本支持更多语言
3. **动画效果**：添加淡入淡出动画，提升视觉体验

### 8.2 交互优化

1. **快捷键自定义**：允许用户自定义触发快捷键
2. **提示样式自定义**：允许用户自定义提示颜色和样式
3. **上下文感知**：根据提交类型（fix、feat 等）显示不同的提示

## 九、参考资源

### 9.1 IntelliJ Platform 文档

- [Inlay Hints API](https://plugins.jetbrains.com/docs/intellij/inlay-hints.html)
- [Editor Custom Element Renderer](https://plugins.jetbrains.com/docs/intellij/inlay-model.html)
- [Action System](https://plugins.jetbrains.com/docs/intellij/basic-action-system.html)

### 9.2 相关实现

- `intelli-ai-engine` 模块中的 `TabHintManager` 和 `TabHintRenderer`
- `NextEditSuggestion` 中的 Inlay 提示实现
- GitHub Copilot 插件的 Inline Suggestion 实现

### 9.3 最佳实践

- 使用 `Disposable` 模式管理资源生命周期
- 遵循 EDT/BGT 线程模型
- 使用 `JBColor` 和 `JBUI` 确保主题兼容性
