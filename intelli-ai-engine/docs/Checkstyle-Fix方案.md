## 问题:

现在我在是用 checkstyle-idea 这个插件来修复代码问题遇到点困难
我现在己经在 checkstyle-idea 这个插件中添加了扩展点，然后在 engine 中实现扩展点，这样 engine 就能拿到扫描出来的问题，包括代码行数和列数，问题描述，
这样我就能直接通过哎来生成修复后的代码，这套流程已经走通了，现在的问题是将 ai 返回的代码正确替换问题代码，因为代码问题可能会涉及到多行，也可能是单行，
你有什么建议？ 给 ai 的提示词应该如何设计？ 应该让 ai 返回什么内容？

## 方案:

这个问题你已经走到 90% 插件作者都会卡住的“最后 10%” 了，而且你现在的难点本质上不是 AI，而是「如何安全、可控地做代码替换」。

我直接给你一个可落地、被多款 AI 插件验证过的方案，分三层讲：

⸻

一、先给结论（非常重要）

❌ 不要让 AI 直接“给我修完后的整个文件”
❌ 不要只靠「行号 + replaceText」
❌ 不要让 AI 自由决定“改哪”

✅ 要做的是：

1. 你（插件）决定“改哪一段”
2. AI 只负责“这一段改成什么”
3. 用可校验的方式应用 patch

换句话说：

定位权在 IDE，生成权在 AI，应用权仍然在 IDE

⸻

二、你现在的真实问题拆解

你已经有了（这一步很关键）：
• 文件路径
• beginLine / beginColumn
• endLine / endColumn
• Checkstyle 的 rule + message

但接下来你会遇到这些坑：

场景 问题
单行违规 AI 改对了，但多改了空格
多行违规 AI 重新排版，行号整体漂移
嵌套结构 if / lambda / stream 缩进被破坏
多处相同代码 替换错位置
并发修复 行号失效

👉 所以：行号 ≠ 可替换边界

⸻

三、正确的整体修复架构（强烈建议）

✅ 推荐的「三段式修复模型」

第 1 步：IDE 精确裁剪“问题代码片段”

你在插件里一定要先做这一步：

originalSnippet = document.getText(
new TextRange(startOffset, endOffset)
)

👉 注意：
• offset 必须由 line+column → offset 计算
• 这是你唯一可信的“锚点”

⸻

第 2 步：AI 只返回「替换片段」

❗这是最关键的设计点
AI 不知道文件结构，也不应该知道

⸻

四、给 AI 的提示词：标准模板（你可以直接用）

✅ System / Developer Prompt（固定）

你是一个 IntelliJ IDEA 插件中的代码修复引擎。
你只允许修改提供的代码片段，不得假设其他上下文。
不要输出解释，只返回修复后的代码片段本身。

⸻

✅ User Prompt（强烈推荐这个结构）

以下是 Checkstyle 检测出的代码问题。

【规则信息】

- 工具：Checkstyle
- Rule：LineLength
- 描述：Line is longer than 120 characters

【原始代码片段】
<<<CODE>>>
public void example() { doSomething(a, b, c, d, e, f, g, h); }
<<<END>>>

【要求】

- 仅修改上述代码片段
- 保持原有语义不变
- 修复该规则问题
- 不引入额外格式化
- 返回【完整替换后的代码片段】

👉 不要给文件内容，不要给上下文 PSI，不要给多余信息

⸻

五、AI 返回什么？（一定要强约束）

❌ 错误返回形式

// 修改后的代码如下
public void example() {
doSomething(...)
}

❌ 包含解释
❌ 包含注释
❌ 包含 markdown
❌ 包含“这里我做了什么”

⸻

✅ 正确返回形式（你唯一应该接受的）

public void example() {
doSomething(
a, b, c, d,
e, f, g, h
);
}

⸻

六、在 IDE 中如何“安全替换”

1️⃣ 替换逻辑（你已经能做）

WriteCommandAction.runWriteCommandAction(project, () -> {
document.replaceString(startOffset, endOffset, aiResult);
});

2️⃣ 替换前的 安全校验（强烈建议）

在替换前做 2 个 check：

✅ 校验 1：原始片段未被用户修改

if (!document.getText(range).equals(originalSnippet)) {
// 放弃修复，提示用户
}

✅ 校验 2：AI 返回非空 & 不包含非法内容
• 非空
• 不包含 ```
• 不包含 “修改如下”
• 不包含 BEGIN / END

⸻


