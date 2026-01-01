# IntelliAI Swagger 方案文档

## 1. 目标与范围

- 目标：在 IntelliJ IDEA 内为 Java REST API 自动生成、更新与导出 Swagger/OpenAPI 文档，使文档始终与代码一致。
- 范围：面向 Spring MVC / Spring Boot 项目，覆盖方法级注解、参数/响应描述、Schema/DTO 注解与文档导出。
- 约束：不修改业务逻辑，仅修改注解与文档；AI 输出必须可插入、可回退、可预览。

## 2. 需求分析

### 2.1 核心需求

- 单接口生成：为 Controller 方法生成 @Operation/@Parameter/@ApiResponses 等注解。
- Schema 自动维护：为被接口引用的 DTO/VO/Request/Response 生成/更新 @Schema。
- 增量更新：仅更新变更字段或参数，保留人工描述。
- 导出能力：按类/目录/Tag 导出 OpenAPI JSON/YAML。

### 2.2 使用场景

- 日常开发：接口新增或变更时快速补全文档。
- 老项目治理：批量补全文档并统一风格。
- 对外接口：按范围导出 OpenAPI 供第三方使用。
- 提交阶段：提交前自动更新接口文档并预览差异。

### 2.3 非功能需求

- 可用性：批量任务不阻塞 UI，支持进度与取消。
- 安全性：AI 输出校验与回退机制。
- 一致性：文档完整性优先，不允许缺失 Schema。

## 3. 调研与结论

- Swagger 注解生态：SpringDoc OpenAPI 3 注解、Knife4j 扩展注解。
- IDEA 扩展点：Action、Intention、Code Vision、Inspection、Commit 工具窗口扩展。
- 结论：采用“统一能力 + 多入口策略”，不同入口仅调整范围与默认策略。

## 4. 架构设计

### 4.1 分层架构

- IDE 接入层：Action/Intention/Code Vision/Commit 入口。
- 代码分析层：PSI 解析 Controller、方法与实体依赖。
- 生成编排层：拆分任务（方法/入参/出参），调度与缓存。
- AI 交互层：模板化 Prompt、输出校验与降级策略。
- 写回层：注解插入、增量更新、Diff 预览与回退。
- 配置层：插件设置、策略与默认行为。

### 4.2 核心数据模型

- ApiMethodDescriptor：方法签名、路径、HTTP 方法、参数、返回值、已有注解快照。
- SchemaGraph：实体依赖图，支持递归解析、去重与变更检测。
- DocGenerationTask：METHOD / REQUEST_SCHEMA / RESPONSE_SCHEMA 任务模型。

## 5. 关键流程

### 5.1 单接口生成

1. 入口触发（意图/右键/Code Vision）。
2. PSI 解析方法与依赖实体。
3. 拆分任务并组装 Prompt。
4. AI 生成注解片段。
5. 写回注解并展示 Diff。

### 5.2 批量生成

1. 目录/文件级扫描接口类。
2. 逐个执行单接口流程。
3. 汇总变更并统一预览。

### 5.3 Commit 阶段更新

1. 获取 Git Diff。
2. 识别受影响接口与实体。
3. 执行增量更新并提示用户确认。

### 5.4 OpenAPI 导出

1. 选择作用域（类/目录/Tag）。
2. 构建 OpenAPI 内部模型。
3. 输出 JSON/YAML，裁剪未引用 Schema。

## 6. AI 生成策略

- 多请求拆分：方法、入参实体、出参实体独立生成。
- Prompt 模板化：系统提示词只读，任务提示词按类型区分。
- 输出约束：仅允许 Java 注解片段，禁止解释性文本。
- 缓存与跳过：未变更实体可跳过，结果可缓存复用。

## 7. 实现方案（与代码结构对齐）

### 7.1 入口层

- Action：右键菜单触发生成。
- Intention：光标处补全文档。
- Code Vision：方法级快速入口。
- Commit：提交面板自动更新。

### 7.2 解析层

- ControllerDetector：识别 Controller 类。
- ApiMethodParser：解析方法签名、参数与注解。
- EntityGraphBuilder：解析 DTO/Schema 依赖。

### 7.3 编排层

- TaskCollector：按作用单元拆分任务。
- TaskExecutor：串行方法任务 + 并行 Schema 任务。
- DiffAnalyzer：对比注解快照，生成增量更新计划。

### 7.4 写回层

- AnnotationWriter：注解插入与属性更新。
- MarkdownWriter：可选生成 Markdown 文档。
- DiffPreview：统一预览与确认。

### 7.5 配置与策略

- 生成策略：仅新增 / 增量更新 / 仅分析。
- Schema 策略：自动补全 / 变更才更新 / 跳过未引用。
- 导出策略：按范围导出、裁剪未引用 Schema。

## 8. MVP 范围与里程碑

### MVP-1（基础可用）

- 入口：右键 Action + Intention。
- 能力：方法级 Swagger 注解生成 + DTO/Schema 自动维护。
- 预览：Diff 预览与确认。

### MVP-2（效率提升）

- Code Vision 快捷入口。
- 文件级批量生成。
- 简化的 OpenAPI 导出。

### MVP-3（治理能力）

- 目录级批量。
- Commit 阶段自动更新。
- Inspection 提示与修复入口。

## 9. 风险与应对

- PSI 插入失败：提供回退与最小插入策略。
- AI 输出不稳定：增强约束、校验与重试。
- 批量性能：并行处理 + 缓存 + 任务队列。

