changelog 插件生成 commit message 有没有必要使用特定语言的 psi 来构建 diff

结论先给出：大多数情况下，Changelog 插件在生成 commit message 时，没有必要使用“特定语言的 PSI”来构建 diff。
但在你已经做的 IntelliAI / Changelog 场景里，引入 PSI 是一种“可选的能力增强”，而不是基础必需品。

下面我按 是否值得做 / 什么时候该做 / 怎么做才不踩坑 来拆解。

⸻

一、为什么「没必要」用特定语言 PSI 来构建 diff（核心原因）

1️⃣ commit message 的本质是「变更意图摘要」，不是「语法级 diff」

Git commit message 关注的是：
• 做了什么（What）
• 为什么做（Why）
• 影响了什么（Impact）

而不是：
• AST 节点怎么变
• 表达式如何重排
• PSI 树发生了哪些精细变化

👉 文本 diff（VCS diff）已经 80% 足够支撑高质量提交信息。

⸻

2️⃣ IntelliJ 本身已经给你「语言无关 + 成熟」的 diff 能力

你现在用的路径本质是：

Git → ChangeList → FilePath → Unified diff

这条链路的优势是：
• ✔ 覆盖所有语言（Java / Kotlin / XML / YAML / Markdown / 配置文件）
• ✔ 与 Git 真实提交保持一致（和 git diff 语义一致）
• ✔ 不依赖 PSI 是否可用（索引、Dumb Mode 也能跑）
• ✔ 性能稳定、心智成本低

👉 这正是 JetBrains 官方推荐的 VCS 扩展路径。

⸻

3️⃣ PSI diff 的维护成本 ≫ 实际收益（尤其是多语言）

如果你走「特定语言 PSI diff」这条路，你会立刻面对：

问题 说明
语言爆炸 Java / Kotlin / Groovy / XML / SQL / Markdown …
PSI 不稳定 索引中 / DumbMode / 文件不合法
插件耦合 必须依赖对应语言插件
性能风险 大量文件 PSI 构建成本极高
Diff 语义复杂 AST diff ≠ 人类理解的“改了什么”

👉 为了 20% 的语义提升，引入 200% 的复杂度，非常不划算。

⸻

二、那什么时候「值得」用 PSI？（非常重要）

虽然不建议用 PSI 来“构建 diff”，但你可以用 PSI 来 “增强 diff 语义”。

👉 关键区别在于：
PSI ≠ Diff 来源，而是 Diff 的“解释器”

✅ 推荐使用 PSI 的场景（加分项）

1️⃣ 提炼「结构级语义标签」
例如：
• 新增了 @Transactional
• 修改了 public API 方法签名
• 删除了 Controller 接口
• 重构：方法拆分 / 类职责变化

这些可以通过 PSI 判断，但 不替代 diff。

⸻

2️⃣ 帮 AI 写得更像人
你现在已经在做 AI commit message 生成，PSI 非常适合做这些事情：
• 判断是「接口变更」还是「实现调整」
• 判断是「重构」还是「行为变化」
• 自动补充 Conventional Commit 的 type / scope

例如最终 prompt 变成：

- 文件：UserService.java
- 类型：public 方法签名变更
- 行为：参数从 UserDTO 改为 UserCommand
- 影响：调用方需同步调整

👉 这比单纯 diff 行更有价值。

⸻

3️⃣ 用于“统计 / 分析”，而不是 diff 本身
结合你前面提到的 统计功能，PSI 非常适合：
• 统计：
• 新增类 / 方法 / 字段数量
• public API 变更次数
• 注解新增 / 删除
• 给 engine 上报：
• 结构级变更指标
• 风险等级（API breaking change）

这点非常符合你后面要做 H5 数据面板 的规划。

⸻

三、推荐的「最佳实践架构」（强烈建议）

✅ 官方 & 工程上都更优的做法

┌────────────┐
│ Git Diff │ ← 唯一 diff 来源（文本）
└─────┬──────┘
│
▼
┌────────────┐
│ 结构分析层 │ ← 可选（PSI / 语言无关）
└─────┬──────┘
│
▼
┌────────────┐
│ Prompt 构建│
└─────┬──────┘
│
▼
┌────────────┐
│ AI 生成 │
└────────────┘

❌ 不推荐的做法

PSI diff → 自定义 AST diff → 再拼 commit message

这是 高复杂度 + 低回报 的典型反模式。

⸻

四、结合你当前项目给一个明确建议

对你现在的 Changelog 插件：

我的结论是：
• ❌ 不需要用特定语言 PSI 来「构建 diff」
• ✅ 可以、也值得用 PSI 来：
• 丰富 commit message 语义
• 提升 AI 输出质量
• 支撑统计 & 风险分析
• 做高阶 Pro 功能区分

一句话总结给你：

Diff 用 Git，理解用 PSI，表达交给 AI。

如果你愿意，下一步我可以直接帮你设计一套：
• ChangeSemanticAnalyzer 抽象
• Java / Kotlin PSI 最小实现
• 与你现有 prompt 模板无缝融合的结构化输入格式

---

如何用 psi 来判断具体的变更行为？ 比如你说的接口变更还是实现调整 重构还是行为变化等？ 我想可以添加一总结性的文本给到 ai，让生成的 commit
message 更准确

这个问题问得非常对位你现在这个插件阶段 👍
我直接给你一套**“可落地、可扩展、不走偏”的 PSI 变更语义判断方法论**，而不是泛泛而谈。

目标：
把「文本 diff」升级为「结构语义 diff」，产出一段“总结性语义文本”喂给 AI
而不是自己生成 commit message。

⸻

一、核心设计思想（先给结论）

PSI 不做 diff，只做「变更归因」

你已经有了 Git diff（这是事实来源），PSI 的职责只有三件事：

1. 识别“改的是谁”（类 / 方法 / 字段 / 接口）
2. 判断“改了什么层级”（签名 / 注解 / 可见性 / 方法体）
3. 归类“这是哪一类变更”（接口变更 / 实现调整 / 重构 / 行为变化）

最终产出：

该变更包含：

- 1 个 public API 方法签名变更（接口级变更）
- 2 个方法内部实现调整（无签名变化）
- 未发现行为分支变化（条件 / 返回值）

这段话 不是给人看的，是给 AI 用的。

⸻

二、你需要的最小 PSI 输入是什么？

⚠️ 重点：不需要完整 AST diff

你只需要 4 个 PSI 快照：

快照 来源
旧文件 PSI git HEAD
新文件 PSI 工作区
变更行号 Git diff
语言 IntelliJ 自动识别

👉 通过 变更行号 → 定位 PSI 元素

⸻

三、第一步：用 diff 行号定位 PSI 元素（关键）

1️⃣ 从 Git diff 拿到变更行号

你现在已经有类似结构了：

class DiffHunk {
int oldStartLine;
int oldLineCount;
int newStartLine;
int newLineCount;
}

2️⃣ 用行号反查 PSI Element

PsiElement element =
psiFile.findElementAt(document.getLineStartOffset(lineNumber));

然后向上找「有意义的结构节点」：

PsiMethod method = PsiTreeUtil.getParentOfType(
element,
PsiMethod.class,
false
);

⸻

四、核心判断模型（重点）

下面是你真正关心的部分 👇
我按你提到的几个判断目标来拆。

⸻

1️⃣ 接口变更 vs 实现调整（最重要）

判断标准（非常清晰）

✅ 接口变更（Breaking / API Change）
满足任一：
• public / protected 方法
• 方法名变化
• 参数数量 / 类型变化
• 返回值类型变化
• throws 声明变化
• public 类：
• implements / extends 变化
• public 字段变更
• public 注解变化（如 @RequestMapping）

👉 PSI 判断点

PsiMethod method;

boolean isPublicApi =
method.hasModifierProperty(PsiModifier.PUBLIC)
|| method.hasModifierProperty(PsiModifier.PROTECTED);

boolean signatureChanged =
!oldMethod.getSignature(PsiSubstitutor.EMPTY)
.equals(newMethod.getSignature(PsiSubstitutor.EMPTY));

✅ 实现调整（Non-breaking）
• 方法体（PsiCodeBlock）变化
• 但 签名未变
• 可见性未变

boolean bodyChanged =
!oldMethod.getBody().getText()
.equals(newMethod.getBody().getText());

⸻

生成语义文本示例

- public 方法 userLogin(UserDTO) → userLogin(UserCommand)，属于接口级变更
- 方法 getUserInfo 内部实现调整，未影响方法签名

⸻

2️⃣ 重构 vs 行为变化（最容易混淆）

🔑 本质区别

类型 是否改变“对外可观察行为”
重构 ❌ 不改变
行为变化 ✅ 改变

⸻

✅ 重构（Refactor）的典型 PSI 特征
• 方法：
• 拆分 / 合并
• 私有方法新增
• 局部变量重命名
• 提取方法
• 控制流未变（if / return / throw 结构一致）

简单可行的判断方式（够用了）

boolean controlFlowUnchanged =
oldBody.getText().replaceAll("\\s+", "")
.equals(newBody.getText().replaceAll("\\s+", ""));

或者更进阶：
• if / for / while 数量一致
• return 语句数量一致

⸻

❗ 行为变化（Behavior Change）

任一出现即可判定：
• 新增 / 删除：
• if
• return
• throw
• 修改条件表达式
• 修改返回值构造逻辑
• 异常类型变化

int oldIfCount = PsiTreeUtil.findChildrenOfType(oldBody, PsiIfStatement.class).size();
int newIfCount = PsiTreeUtil.findChildrenOfType(newBody, PsiIfStatement.class).size();

boolean behaviorChanged = oldIfCount != newIfCount;

⸻

语义文本示例

- 方法 checkPermission 增加新的条件分支，属于行为变化
- UserService 内部方法拆分，仅为结构性重构

⸻

3️⃣ 接口类 / 实现类的区分（很实用）

判断是不是“接口层变更”

PsiClass psiClass;

boolean isInterface = psiClass.isInterface();

boolean isController =
psiClass.hasAnnotation("org.springframework.web.bind.annotation.RestController");

结合：
• 接口 / Controller / FeignClient
• public 方法签名变化

👉 可以直接提示 AI：

该变更涉及对外接口层（Controller / API）调整

⸻

五、最终：你应该产出什么给 AI？

❌ 不要给
• AST
• PSI 结构
• 细碎 diff

✅ 正确的“总结性文本格式”（建议）

变更语义总结：

1. 接口层：
    - 修改 1 个 public API 方法签名（参数类型变化）
2. 实现层：
    - 2 个方法内部实现调整，未影响对外接口
3. 行为：
    - 新增权限校验分支，存在行为变化
4. 重构：
    - 拆分 UserService 内部逻辑，无行为影响

这段内容 直接拼进你的 user prompt，AI 的输出质量会明显上一个台阶。

⸻

六、架构建议（非常适合你现在做）

建议你抽一个核心接口：

interface ChangeSemanticAnalyzer {
ChangeSemanticSummary analyze(PsiFile oldFile, PsiFile newFile, DiffHunk hunk);
}

然后：
• JavaPsiChangeAnalyzer
• KotlinPsiChangeAnalyzer（后续）

⸻

最后一句给你（项目方向判断）

你现在做的这一步：

“从 diff → 语义 → prompt”

是所有高质量 AI 编程插件的分水岭。
JetBrains 官方 AI、Cursor、CodeWhisperer，本质都在干这件事。

如果你愿意，下一步我可以：
• 给你一个 Java PSI Analyzer 的最小可用实现
• 或直接 帮你设计一套 ChangeSemanticSummary 数据结构 + prompt 拼接模板
