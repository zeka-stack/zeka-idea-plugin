## 版本 2025.3.1100

### 2026-01-19

#### ⭐ 新功能

- 实现前端页面与路由，新增 Home、FeatureRequests、PrivacyPolicy、Statistics 页面，并重新组织入口逻辑。
- 在 zeka-stack-api 中新增事件统计表、MySQL 数据源配置与测试支持。
- 在 core 层增加事件统计接口、DTO、XML 映射文件。
- 在 intelli-ai-javadoc 中加入 Javadoc 统计上报与 AI 使用量跟踪。
- 在 intelli-ai-engine 统计模块中扩展支持的 AI 服务商列表，并对 Provider 注释做优化。
- 更新 intelli-ai-changelog 插件版本，新增快速切换服务商、修复 diff 生成错误以及统一 diff 解析。

#### 🪲 问题修复

- 修复 intelli-ai-changelog 中新增或删除文件的 diff 生成错误。

#### ♻️ 代码重构

- 重构 intelli-ai-changelog 主模块，新增 AI 使用量监听器与统计上报。
- 重构 intelli-ai-engine 统计模块，加入设备 ID 生成器、统计配置 UI、枚举等，并提升性能。
- 优化 AI 服务商配置默认值与加载逻辑，提升兼容性。

#### 📓 文档更新

- 无

#### 🔧 其他改进

- 在 intelli-ai-javadoc 中更新插件版本号、优化 AIRequestComposer、添加 ProjectVersionResolver 等工具。

---

### 2026-01-18

#### ⭐ 新功能

- 添加 AzureOpenAI、Bedrock、GitHubModels、Mistral 等 OpenAI 兼容提供者及图标资源。
- 新增 OpenRouter 与 Cloudflare OpenAI 提供商支持，并提供相应图标。
- 引入 NVIDIA 与 HuggingFace OpenAI 兼容提供商，扩展 AIProviderType 与工厂实例化。
- 在插件中加入 AI 平台图标资源文件。

#### 🪲 问题修复

- 优化反馈提交接口路径与响应结构，修复业务层成功判断逻辑。

#### ♻️ 代码重构

- 抽离提交消息动作至服务层，新增分割按钮支持。
- 重构 SiliconFlow 图标资源统一尺寸缩放逻辑。
- 统一 AI 通用图标类，支持新增提供商并统一管理。
- 重构差异负载构建逻辑，简化新增/删除处理。

#### 📓 文档更新

- 无

#### 🔧 其他改进

- 初始化前端项目结构，添加基础组件与配置文件。
- 删除 feedback-server 项目全部文件（已忽略）。

---

### 2026-01-17

#### ⭐ 新功能

- 在 intelli-ai-changelog 添加删除/新增文件差异处理与摘要优化。
- 在 zeka-stack-api 初始化反馈与模型功能，支持 GitHub 讨论、签名验证等。
- 在 intelli-ai-engine 扩展 AI 提供商支持（OpenAI、Anthropic、ModelScope 等），改进 UI。

#### 🪲 问题修复

- 无

#### ♻️ 代码重构

- 无

#### 📓 文档更新

- 重新组织 zeka-stack-api 文档并更新 API 示例。

#### 🔧 其他改进

- 无

---

### 2026-01-16

#### ⭐ 新功能

- 在 intelli-ai-changelog 添加 AI 生成提交、合并功能，并自动解析项目版本号。
- 在 intelli-ai-engine 加入 Claude、Gemini、Codex、GLM 支持，并改进模型选择器。

#### 🪲 问题修复

- 无

#### ♻️ 代码重构

- 无

#### 📓 文档更新

- 无

#### 🔧 其他改进

- 无
