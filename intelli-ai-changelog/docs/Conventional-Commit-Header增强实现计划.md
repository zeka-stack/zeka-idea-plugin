# Conventional Commit Header 增强 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `intelli-ai-changelog` 的 Git Commit Message 输入框中，对 Conventional Commit Header（`type(scope)!: subject`）做分段高亮，并提供 type / 轻量 scope / `!` 自动补全。

**Architecture:** 不引入自定义 Language。用正则解析首行 Header，`MarkupModel` 着色，`CompletionContributor(language=TEXT)` 仅在 Commit Message 编辑器生效；与现有 Inlay Hint / AI 生成并列，设置总开关控制。

**Tech Stack:** IntelliJ Platform（Editor / MarkupModel / CompletionContributor）、JUnit 5 + AssertJ、现有 `ChangelogGitService` / `SettingsState` / `ChangelogBundle`。

**Spec:** `intelli-ai-changelog/docs/Conventional-Commit-Header增强实现方案.md`

## Global Constraints

- 范围 C：Header 分段高亮 + type/scope/`!` 补全；**不补 subject/body/footer**
- 不做自定义 Language、Inspection、Template、JSON tokens
- Type 白名单与 prompt 一致：`feat, fix, refactor, perf, docs, test, build, chore, style, revert`
- UI 更新在 EDT；Git 读取在 BGT
- 所有用户可见文案走 `ChangelogBundle`（中英）
- 代码修改需用户确认后再动；改完用 `./compile.sh intelli-ai-changelog` 编译
- Git commit **仅在用户明确要求时**执行（本计划中的 commit 步骤默认跳过，除非用户要求）

---

## File Structure

### Create

| File | Responsibility |
|------|----------------|
| `.../conventional/ConventionalCommitHeaderParser.java` | 解析首行 Header，输出带 TextRange 的 tokens + 光标上下文枚举 |
| `.../conventional/ConventionalCommitHeader.java` | 不可变解析结果（type/scope/breaking/separator/subject ranges） |
| `.../conventional/ConventionalCommitContext.java` | 光标所在段：`TYPE` / `SCOPE` / `BREAKING` / `SUBJECT` / `OTHER` |
| `.../conventional/ConventionalCommitTypes.java` | 标准 type 列表与 `isStandard(type)` |
| `.../conventional/ConventionalCommitColors.java` | `TextAttributesKey` 定义 |
| `.../conventional/ConventionalCommitHighlighter.java` | 对 Editor 首行挂/刷新 RangeHighlighter |
| `.../conventional/ConventionalCommitEditorSupport.java` | 监听 Document，防抖刷新高亮；随 Editor dispose |
| `.../conventional/ConventionalCommitEditorFactoryListener.java` | 识别 Commit Message Editor 并挂载 Support |
| `.../conventional/ConventionalCommitCompletionContributor.java` | type / scope / `!` 补全 |
| `.../conventional/ConventionalCommitScopeProvider.java` | 项目级缓存近期 scopes |
| `.../conventional/ConventionalCommitHeaderParserTest.java` | Parser 单测 |
| `.../conventional/ConventionalCommitTypesTest.java` | Types 单测 |

### Modify

| File | Change |
|------|--------|
| `META-INF/plugin.xml` | 注册 listener + completion.contributor |
| `settings/SettingsState.java` | `enableConventionalCommitAssist = true` |
| `settings/ui/ChangelogSettingsPanel.java` | 勾选框 + isModified/apply/reset |
| `messages/ChangelogBundle.properties` | 英文文案 |
| `messages/ChangelogBundle_zh_CN.properties` | 中文文案 |
| `service/ChangelogGitService.java` | 暴露 public 方法供 scope 缓存读取近期提交 |
| `includes/pluginChanges.html` | 版本更新记录（实现完成后） |
| `site/docs/用户手册.md` | Commit Message 章节补充（实现完成后） |

---

### Task 1: Header Parser + Types

**Files:**
- Create: `intelli-ai-changelog/src/main/java/dev/dong4j/zeka/stack/idea/plugin/changelog/conventional/ConventionalCommitHeader.java`
- Create: `intelli-ai-changelog/src/main/java/dev/dong4j/zeka/stack/idea/plugin/changelog/conventional/ConventionalCommitContext.java`
- Create: `intelli-ai-changelog/src/main/java/dev/dong4j/zeka/stack/idea/plugin/changelog/conventional/ConventionalCommitHeaderParser.java`
- Create: `intelli-ai-changelog/src/main/java/dev/dong4j/zeka/stack/idea/plugin/changelog/conventional/ConventionalCommitTypes.java`
- Test: `intelli-ai-changelog/src/test/java/dev/dong4j/zeka/stack/idea/plugin/changelog/conventional/ConventionalCommitHeaderParserTest.java`
- Test: `intelli-ai-changelog/src/test/java/dev/dong4j/zeka/stack/idea/plugin/changelog/conventional/ConventionalCommitTypesTest.java`

**Interfaces:**
- Produces:
  - `ConventionalCommitHeaderParser.parseFirstLine(CharSequence documentText) -> ConventionalCommitHeader`
  - `ConventionalCommitHeaderParser.contextAt(ConventionalCommitHeader header, int offsetInFirstLine) -> ConventionalCommitContext`
  - `ConventionalCommitTypes.ALL` / `isStandard(String)` / `matchesPrefix(String prefix)`
  - `ConventionalCommitHeader` fields: nullable ranges for type/scope/breaking/separator/subject；`firstLine`；`firstLineEndExclusive`

- [ ] **Step 1: Write failing parser tests**

```java
package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConventionalCommitHeaderParserTest {

    @Test
    void shouldParseFullHeader() {
        ConventionalCommitHeader header =
            ConventionalCommitHeaderParser.parseFirstLine("feat(changelog)!: add highlight\n\n- body");

        assertThat(header.type()).isEqualTo("feat");
        assertThat(header.scope()).isEqualTo("changelog");
        assertThat(header.hasBreakingChange()).isTrue();
        assertThat(header.subject()).isEqualTo(" add highlight");
        assertThat(header.typeRange()).isEqualTo(TextRange.create(0, 4));
    }

    @Test
    void shouldParseTypeOnlyPartialInput() {
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine("fe");
        assertThat(header.type()).isEqualTo("fe");
        assertThat(header.scope()).isNull();
        assertThat(header.hasSeparator()).isFalse();
        assertThat(ConventionalCommitHeaderParser.contextAt(header, 2))
            .isEqualTo(ConventionalCommitContext.TYPE);
    }

    @Test
    void shouldDetectScopeContextInsideParens() {
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine("feat(ch");
        assertThat(ConventionalCommitHeaderParser.contextAt(header, 6))
            .isEqualTo(ConventionalCommitContext.SCOPE);
    }

    @Test
    void shouldIgnoreBodyLines() {
        ConventionalCommitHeader header =
            ConventionalCommitHeaderParser.parseFirstLine("fix: x\n\n- not header");
        assertThat(header.type()).isEqualTo("fix");
        assertThat(header.subject()).isEqualTo(" x");
    }

    @Test
    void shouldTreatSubjectAsSubjectContext() {
        ConventionalCommitHeader header =
            ConventionalCommitHeaderParser.parseFirstLine("docs: update readme");
        int offset = "docs: ".length();
        assertThat(ConventionalCommitHeaderParser.contextAt(header, offset))
            .isEqualTo(ConventionalCommitContext.SUBJECT);
    }
}
```

- [ ] **Step 2: Write Types test**

```java
@Test
void shouldExposePromptAlignedWhitelist() {
    assertThat(ConventionalCommitTypes.ALL).containsExactly(
        "feat", "fix", "refactor", "perf", "docs",
        "test", "build", "chore", "style", "revert"
    );
    assertThat(ConventionalCommitTypes.isStandard("feat")).isTrue();
    assertThat(ConventionalCommitTypes.isStandard("ci")).isFalse();
    assertThat(ConventionalCommitTypes.matchesPrefix("f"))
        .contains("feat", "fix");
}
```

- [ ] **Step 3: Run tests — expect FAIL (classes missing)**

```bash
cd intelli-ai-changelog && ./gradlew test --tests "*.ConventionalCommitHeaderParserTest" --tests "*.ConventionalCommitTypesTest"
```

Expected: compilation failure / class not found.

- [ ] **Step 4: Implement model + parser + types**

核心解析思路（实现时可微调，但行为必须过测）：

```java
// ConventionalCommitContext.java
public enum ConventionalCommitContext {
    TYPE, SCOPE, BREAKING, SUBJECT, OTHER
}

// ConventionalCommitHeader — record 或不可变类，含：
// String type/scope/subject (nullable)
// TextRange typeRange/scopeRange/breakingRange/separatorRange/subjectRange (nullable)
// boolean hasBreakingChange / hasSeparator
// int firstLineEndExclusive

// Parser 建议：
// 1) 取第一行（到 \n 前）
// 2) 用正则匹配：
//    ^(?<type>[^\s(:!]+)(?<scope>\([^)\r\n]*\)?)?(?<breaking>!)?(?<sep>:)?(?<subject>.*)?$
// 3) scope 去掉包围括号；未闭合 `(xxx` 仍算 scope 段
// 4) contextAt：按 range.contains(offset) 优先级 TYPE→SCOPE→BREAKING→SEPARATOR→SUBJECT
//    若在 type 之后、`:` 之前且无 `!`，offset 刚好在 breaking 可插入位 → BREAKING
```

`ConventionalCommitTypes`：

```java
public final class ConventionalCommitTypes {
    public static final List<String> ALL = List.of(
        "feat", "fix", "refactor", "perf", "docs",
        "test", "build", "chore", "style", "revert"
    );

    public static boolean isStandard(@NotNull String type) {
        return ALL.contains(type);
    }

    @NotNull
    public static List<String> matchesPrefix(@NotNull String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return ALL.stream().filter(t -> t.startsWith(p)).toList();
    }
}
```

类需补模块/类级 Javadoc（说明为何只解析首行）。

- [ ] **Step 5: Run tests — expect PASS**

```bash
cd intelli-ai-changelog && ./gradlew test --tests "*.ConventionalCommitHeaderParserTest" --tests "*.ConventionalCommitTypesTest"
```

- [ ] **Step 6: Commit only if user asks**

---

### Task 2: Highlighter + Editor wiring

**Files:**
- Create: `.../conventional/ConventionalCommitColors.java`
- Create: `.../conventional/ConventionalCommitHighlighter.java`
- Create: `.../conventional/ConventionalCommitEditorSupport.java`
- Create: `.../conventional/ConventionalCommitEditorFactoryListener.java`
- Modify: `intelli-ai-changelog/src/main/resources/META-INF/plugin.xml`

**Interfaces:**
- Consumes: `ConventionalCommitHeaderParser`, `ConventionalCommitTypes`, `SettingsState.enableConventionalCommitAssist`, `CommitMessageHintService.isCommitMessageEditor`
- Produces: `ConventionalCommitEditorSupport.attach(Editor)`；文档变更后防抖刷新首行高亮

- [ ] **Step 1: Implement colors**

```java
public final class ConventionalCommitColors {
    public static final TextAttributesKey TYPE =
        TextAttributesKey.createTextAttributesKey("CHANGELOG_CC_TYPE");
    public static final TextAttributesKey TYPE_UNKNOWN =
        TextAttributesKey.createTextAttributesKey("CHANGELOG_CC_TYPE_UNKNOWN");
    public static final TextAttributesKey SCOPE =
        TextAttributesKey.createTextAttributesKey("CHANGELOG_CC_SCOPE");
    public static final TextAttributesKey BREAKING =
        TextAttributesKey.createTextAttributesKey("CHANGELOG_CC_BREAKING");
    public static final TextAttributesKey SEPARATOR =
        TextAttributesKey.createTextAttributesKey("CHANGELOG_CC_SEPARATOR");
    public static final TextAttributesKey SUBJECT =
        TextAttributesKey.createTextAttributesKey("CHANGELOG_CC_SUBJECT");
}
```

在 `ConventionalCommitHighlighter` 初始化时，为 key 设置默认 `TextAttributes`（前景色即可；标准 type 用偏蓝，unknown 用灰色，breaking 用警告红/橙，scope 用青绿）。

- [ ] **Step 2: Implement Highlighter**

```java
public final class ConventionalCommitHighlighter implements Disposable {
    private final Editor editor;
    private final List<RangeHighlighter> highlighters = new ArrayList<>();

    public void refresh() {
        clear();
        if (editor.isDisposed()) return;
        Document doc = editor.getDocument();
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine(doc.getCharsSequence());
        MarkupModel model = editor.getMarkupModel();
        // 对每个非空 range 添加 RangeHighlighter，layer = HighlighterLayer.SYNTAX
        // type: isStandard ? TYPE : TYPE_UNKNOWN
    }

    private void clear() { /* remove all tracked highlighters */ }

    @Override
    public void dispose() { clear(); }
}
```

- [ ] **Step 3: Implement EditorSupport（防抖 + 开关）**

```java
public final class ConventionalCommitEditorSupport implements Disposable {
    private static final int DEBOUNCE_MS = 80;
    private final Editor editor;
    private final ConventionalCommitHighlighter highlighter;
    private final Alarm alarm; // SWING_THREAD

    public static void attach(@NotNull Editor editor, @NotNull Disposable parent) {
        if (!SettingsState.getInstance().enableConventionalCommitAssist) return;
        // create support, Disposer.register(parent, support)
        // documentListener → alarm.cancelAllRequests + alarm.addRequest(refresh, DEBOUNCE_MS)
        // initial refresh on EDT
    }
}
```

注意：设置关闭时不挂载；若用户中途关闭设置，可在下次打开 Commit 框时自然不挂（本版不强制热切换，可接受）。

- [ ] **Step 4: EditorFactoryListener**

复用识别逻辑：

```java
@Override
public void editorCreated(@NotNull EditorFactoryEvent event) {
    Editor editor = event.getEditor();
    Project project = editor.getProject();
    if (project == null || project.isDisposed()) return;
    ApplicationManager.getApplication().invokeLater(() -> {
        if (project.isDisposed() || editor.isDisposed()) return;
        if (!CommitMessageHintService.isCommitMessageEditor(editor)) return;
        ConventionalCommitEditorSupport.attach(editor, project);
    }, project.getDisposed());
}
```

- [ ] **Step 5: Register in plugin.xml**

在现有 `editorFactoryListener`（Hint）旁增加：

```xml
<editorFactoryListener
    implementation="dev.dong4j.zeka.stack.idea.plugin.changelog.conventional.ConventionalCommitEditorFactoryListener"/>
```

- [ ] **Step 6: Compile**

```bash
./compile.sh intelli-ai-changelog
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Manual smoke（可选本任务）**：`runIde` 打开 Commit 框输入 `feat(changelog)!: x`，确认分段着色

- [ ] **Step 8: Commit only if user asks**

---

### Task 3: Completion Contributor（type + `!`）

**Files:**
- Create: `.../conventional/ConventionalCommitCompletionContributor.java`
- Modify: `META-INF/plugin.xml`

**Interfaces:**
- Consumes: Parser / Types / `CommitMessageHintService` / `SettingsState`
- Produces: BASIC 补全；TYPE 段补 types；BREAKING 位补 `!`；SUBJECT 段 **无** 建议

- [ ] **Step 1: Implement contributor**

```java
public class ConventionalCommitCompletionContributor extends CompletionContributor implements DumbAware {
    public ConventionalCommitCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters,
                                          @NotNull ProcessingContext context,
                                          @NotNull CompletionResultSet result) {
                if (!SettingsState.getInstance().enableConventionalCommitAssist) return;
                Editor editor = parameters.getEditor();
                if (!CommitMessageHintService.isCommitMessageEditor(editor)) return;

                Document document = editor.getDocument();
                int offset = parameters.getOffset();
                int line = document.getLineNumber(offset);
                if (line != 0) return; // 只处理首行

                int lineStart = document.getLineStartOffset(0);
                int offsetInLine = offset - lineStart;
                String firstLine = document.getText(TextRange.create(lineStart, document.getLineEndOffset(0)));
                ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine(firstLine);
                ConventionalCommitContext ctx =
                    ConventionalCommitHeaderParser.contextAt(header, offsetInLine);

                CompletionResultSet rs = result.caseInsensitive();
                switch (ctx) {
                    case TYPE -> {
                        String prefix = header.type() == null ? "" :
                            firstLine.substring(0, Math.min(offsetInLine, firstLine.length()));
                        // 更稳妥：用 header.typeRange 截取到光标的前缀
                        for (String type : ConventionalCommitTypes.matchesPrefix(typePrefix)) {
                            rs.addElement(LookupElementBuilder.create(type)
                                .withTypeText("commit type")
                                .withInsertHandler((ctx2, item) -> {
                                    // 若插入后没有 ':'，则补 ": "
                                }));
                        }
                    }
                    case BREAKING -> rs.addElement(LookupElementBuilder.create("!")
                        .withTypeText("breaking change"));
                    case SCOPE, SUBJECT, OTHER -> { /* no-op for subject; scope in Task 4 */ }
                }
            }
        });
    }
}
```

InsertHandler 约束：

- 选中 `feat` 后，若当前位置后没有 `:`，插入 `feat: `（或 type + `: `）
- 若已有 `:`，只替换 type 段文本

- [ ] **Step 2: Register**

```xml
<completion.contributor
    language="TEXT"
    order="first"
    implementationClass="dev.dong4j.zeka.stack.idea.plugin.changelog.conventional.ConventionalCommitCompletionContributor"/>
```

- [ ] **Step 3: Compile**

```bash
./compile.sh intelli-ai-changelog
```

- [ ] **Step 4: Hand test**：输入 `f` → Ctrl+Space → 选 `feat` → 得到 `feat: `

- [ ] **Step 5: Commit only if user asks**

---

### Task 4: Light Scope Provider + Scope completion

**Files:**
- Create: `.../conventional/ConventionalCommitScopeProvider.java`
- Modify: `.../service/ChangelogGitService.java`（暴露 public API）
- Modify: `ConventionalCommitCompletionContributor.java`（SCOPE 分支）

**Interfaces:**
- Consumes: `ChangelogGitService` 近期提交文本；`ConventionalCommitHeaderParser` 抽 scope
- Produces: `ConventionalCommitScopeProvider.getInstance(project).getRecentScopes() -> List<String>`

- [ ] **Step 1: Expose git recent messages**

在 `ChangelogGitService` 增加 public 方法（或将现有 package-private 方法改为 public）：

```java
@NotNull
public String buildRecentCommitMessagesTextPublic(int limit) {
    return buildRecentCommitMessagesText(limit);
}
```

更好：直接把现有 `buildRecentCommitMessagesText` 改为 `public`（若无调用方允许）。

- [ ] **Step 2: Implement ScopeProvider（project service 或静态缓存）**

```java
public final class ConventionalCommitScopeProvider {
    private static final int LIMIT = 30;
    private final Project project;
    private volatile List<String> cached = List.of();
    private volatile long cachedAt;

    public void refreshAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ChangelogGitService git = /* get for project */;
            String text = git.buildRecentCommitMessagesText(LIMIT);
            List<String> scopes = extractScopes(text); // 用 HeaderParser 解析每行 type(scope):
            cached = scopes;
            cachedAt = System.currentTimeMillis();
        });
    }

    @NotNull
    public List<String> getRecentScopes() {
        if (System.currentTimeMillis() - cachedAt > 60_000) {
            refreshAsync(); // 过期触发刷新，本次仍返回旧缓存
        }
        return cached;
    }
}
```

`extractScopes`：对每行 `^-?\\s*(.+)$` 取 message，`parseFirstLine`，收集非空 scope，按出现频次排序去重。

可在 `EditorSupport.attach` 时触发一次 `refreshAsync()`。

- [ ] **Step 3: Wire SCOPE completion**

```java
case SCOPE -> {
    String prefix = /* scope text before caret */;
    for (String scope : provider.getRecentScopes()) {
        if (scope.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
            rs.addElement(LookupElementBuilder.create(scope).withTypeText("scope"));
        }
    }
}
```

缓存为空时不报错、不加假数据。

- [ ] **Step 4: Compile + hand test**（有历史 `feat(xxx):` 时，在 `feat(|)` 内应看到 xxx）

- [ ] **Step 5: Commit only if user asks**

---

### Task 5: Settings switch + i18n

**Files:**
- Modify: `settings/SettingsState.java`
- Modify: `settings/ui/ChangelogSettingsPanel.java`
- Modify: `messages/ChangelogBundle.properties`
- Modify: `messages/ChangelogBundle_zh_CN.properties`

- [ ] **Step 1: Add field**

```java
/** 是否启用 Conventional Commit Header 高亮与 type/scope 补全 */
public boolean enableConventionalCommitAssist = true;
```

放在 Commit Message 相关字段附近（`enableCommitMultiRepoCheck` 旁）。

- [ ] **Step 2: Bundle keys**

`ChangelogBundle.properties`:

```properties
settings.commit.conventional.assist.enable=✨ Highlight & complete Conventional Commit headers (type/scope)
```

`ChangelogBundle_zh_CN.properties`:

```properties
settings.commit.conventional.assist.enable=✨ 高亮并补全 Conventional Commit 前缀（type/scope）
```

- [ ] **Step 3: Panel wiring**

在 Commit Message 设置区块（`enableCommitMultiRepoCheckBox` 附近）增加 `JBCheckBox`，并接入：

- 构造初始化
- `isModified`
- `apply`
- `reset`/`loadFrom`

- [ ] **Step 4: Compile**

```bash
./compile.sh intelli-ai-changelog
```

- [ ] **Step 5: Commit only if user asks**

---

### Task 6: Docs + changelog notes

**Files:**
- Modify: `intelli-ai-changelog/includes/pluginChanges.html`
- Modify: `intelli-ai-changelog/site/docs/用户手册.md`（方式六：生成提交信息附近）

- [ ] **Step 1: pluginChanges.html**

在文件顶部按当前版本规范新增中英文条目，例如：

```html
<li><b>✨ 新增</b>：Commit Message 输入框支持 Conventional Commit Header 分段高亮与 type/scope 自动补全</li>
```

```html
<li><b>✨ Added</b>: Conventional Commit header highlighting and type/scope completion in the commit message box</li>
```

版本号按发布约定选择（实现时看 `gradle.properties` 当前版本，不要臆造未约定版本）。

- [ ] **Step 2: 用户手册**

在「生成提交信息」章节补充：

- 输入框会对 `feat(scope)!: subject` 分段着色
- Ctrl+Space 可补全标准 type 与近期 scope
- 设置路径与关闭开关说明

- [ ] **Step 3: Final compile**

```bash
./compile.sh intelli-ai-changelog
```

- [ ] **Step 4: Commit only if user asks**

---

## Manual Test Checklist（全部任务完成后）

- [ ] Modal Commit：输入 `feat(changelog)!: add x` 分段着色正确
- [ ] 输入 `f` 补全得到 `feat: `
- [ ] `feat(|)` 在有历史 scope 时能补全
- [ ] subject 段 Ctrl+Space **不**出现本功能的假建议（或至少不插入 type）
- [ ] AI 生成后首行仍正确着色
- [ ] Inlay Hint + Tab 生成仍可用
- [ ] 关闭设置开关后不再高亮/补全
- [ ] Non-modal Commit 工具窗口同样生效

---

## Spec Coverage Self-Review

| Spec 项 | Task |
|---------|------|
| Header 解析 + ranges | Task 1 |
| type/scope/`!`/subject 高亮 | Task 2 |
| type / `!` 补全，不补 subject | Task 3 |
| 轻量 scope 补全 | Task 4 |
| 设置总开关 + i18n | Task 5 |
| 独立 EditorFactoryListener | Task 2 |
| completion.contributor TEXT | Task 3 |
| EDT/BGT | Task 2/4 |
| pluginChanges + 手册 | Task 6 |
| 不做 Language/Inspection/Template/JSON | 全任务遵守 |

---

## Execution Handoff

Plan 已保存到：

`intelli-ai-changelog/docs/Conventional-Commit-Header增强实现计划.md`

两种执行方式：

1. **Subagent-Driven（推荐）** — 每个 Task 开一个子 agent，任务间我来 review  
2. **Inline Execution** — 本会话按任务连续实现，关键节点停下来给你看

你选哪种？确认后我才开始改代码。
