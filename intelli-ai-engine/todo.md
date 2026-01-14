## 2025.11.24

- [x] 将模型参数与添加到可用列表的模型关联起来, 而不是共用同一套模型参数
- [x] Ollama LMStudio 不强制限制是否输入 api key

## 2025.11.27

- [x] 超时时间单位改成秒
- [x] token 最大长度单位改成 K
- [ ] 模型下拉列表支持搜索筛选(忽略大小写)
- [x] 添加 modelscope 服务商

获取支持的模型:

PUT https://modelscope.cn/api/v1/dolphin/models

{"PageSize":100,"PageNumber":1,"SortBy":"Default","Target":"","Criterion":[{"category":"tasks","predicate":"contains","
values":["text-generation"],"sub_values":[]}],"
SingleCriterion":[{"category":"inference_type","DateType":"int","predicate":"equal","IntValue":1}]}

响应结果: 取 model_id

```json
{
    "Code": 200,
    "Data": {
        "Model": {
            "Models": [
                {

                    "BackendSupport": {
                        "model_id": "ZhipuAI/GLM-4.6"
                    },
                    "Organization": {
                        "Avatar": "https://resouces.modelscope.cn/avatar/0087d595-e6b3-4a6e-bc23-12d62209f0df.webp"
                    }
                },
                {
                    "BackendSupport": {
                        "model_id": "Qwen/Qwen3-Next-80B-A3B-Instruct"
                    },
				    "Organization": {
                        "Avatar": "https://resouces.modelscope.cn/avatar/40c50d63-9e30-4589-9b79-0e46800b5cc3.png"
                    }
                }
            ],
            "TotalCount": 51
        },
        "Source": "",
        "Target": ""
    },
    "Message": "success",
    "RequestId": "31a85a62-6721-4e2b-aaac-af40fbdb11c7",
    "Success": true
}
```

https://api-inference.modelscope.cn/v1/chat/completions

- [ ] ~~对接 Dify~~
- [ ] ~~多模态支持~~

## 2025.12.30

- [x] 支持 SSE

## 2025.12.31

- [x] 将自定义语言从 javadoc 中迁移到 engine 中, 这样所有子插件就可以使用


## 2026.01.05

- [x] 修复过时 API

```
1 使用计划移除 API
IntelliAI Engine 2025.3.1使用的 API 计划在未来的版本中移除。

计划采用移除方法（1 ）
SystemInfo.getOsNameAndVersion() （1 ） （计划在 未来的版本中移除）
此方法已弃用SystemInfo.getOsNameAndVersion()，FeedbackPanel.getOperatingSystem()将在未来的版本中移除。
3 种已弃用的 API用法
IntelliAI Engine 2025.3.1使用了已弃用的 API，该 API 可能会在未来的版本中被移除，从而导致二进制文件和源代码不兼容。

已弃用的类用法（1 ）
TipUIUtil （1 ）
已弃用的类TipUIUtil在以下位置被引用WhatsNewPanel.<init>()
已弃用的接口用法（1 ）
EdtScheduledExecutorService （1 ）
已弃用的接口EdtScheduledExecutorService在以下位置被引用：WhatsNewStartupActivity.execute(...)
已弃用的方法用法（1 ）
Presentation.putClientProperty(String, Object) （1 ）
已弃用的方法Presentation.putClientProperty(String, Object)被调用AIStatusBarWidget.OutputLanguageAction.update(...)
```

## 2026.01.07

- [x] 适配 minimax 流式输出格式

定时检查问题:

2026-01-12 11:51:42,967 [5565287]   FINE - dev.dong4j.zeka.stack.idea.plugin.common.agent.IntelliAgentManager - 检查端口 8765
上的服务失败: http://127.0.0.1:8765/health
java.net.ConnectException: Connection refused
at java.base/sun.nio.ch.Net.pollConnect(Native Method)
at java.base/sun.nio.ch.Net.pollConnectNow(Net.java:682)
at java.base/sun.nio.ch.NioSocketImpl.timedFinishConnect(NioSocketImpl.java:549)
at java.base/sun.nio.ch.NioSocketImpl.connect(NioSocketImpl.java:592)
at java.base/java.net.Socket.connect(Socket.java:751)
at java.base/sun.net.NetworkClient.doConnect(NetworkClient.java:178)
at java.base/sun.net.www.http.HttpClient.openServer(HttpClient.java:531)
at java.base/sun.net.www.http.HttpClient.openServer(HttpClient.java:636)

## 2026.01.14

集成 OpenCode 相同的 AI 服务商

## 2026.01.14

尝试使用 IDEA 的 LSP 服务看看有没有创新的特性

1. AI 增强的代码补全

混合补全引擎：LSP 提供基于上下文的补全 + AI 提供语义理解补全
智能补全排序：用 AI 分析代码意图，对 LSP 补全结果重新排序
长文本补全：LSP 处理短补全，AI 处理需要理解的复杂代码生成

2. AI 驱动的代码质量检查

深度诊断分析：LSP 提供基础语法错误，AI 分析潜在业务逻辑问题
架构层面建议：结合 LSP 的符号查找，AI 分析代码结构和依赖关系
性能优化建议：基于 LSP 的代码分析，AI 提供性能改进方案

3. 智能代码重构

安全重构：使用 LSP 的 find references 确保重构完整性，AI 生成优化方案
跨文件重构：LSP 提供符号依赖图，AI 生成重构代码
代码风格统一：LSP 识别代码模式，AI 自动统一风格

4. 上下文感知的代码解释

LSP 符号信息 + AI 解释：点击代码元素，LSP 提供定义，AI 解释业务逻辑
文档自动生成：LSP 分析代码结构，AI 生成符合规范的文档
代码审查：LSP 识别修改范围，AI 进行深度审查

5. 智能导航增强

语义跳转：LSP 提供定义位置，AI 理解业务上下文，提供更精准的跳转建议
相关代码推荐：基于 LSP 的 workspace/symbol，AI 推荐相关功能代码
影响范围分析：LSP 找引用，AI 分析修改影响和测试建议

6. AI 辅助调试

智能断点建议：LSP 分析代码执行路径，AI 预测关键断点位置
错误解释：LSP 提供错误位置，AI 解释原因和解决方案
日志分析：结合 LSP 的代码结构，AI 分析日志问题

7. 自动化测试生成

测试用例推荐：LSP 识别代码边界，AI 生成测试用例
Mock 数据生成：LSP 分析接口定义，AI 生成测试数据
测试覆盖率分析：LSP 标记未测试代码，AI 生成补充测试

8. 代码迁移助手

API 升级：LSP 识别旧 API 调用，AI 生成新 API 代码
语言转换：LSP 解析源代码，AI 生成目标语言代码
框架迁移：LSP 分析依赖关系，AI 生成迁移方案

9. 团队协作增强

Code Review 助手：LSP 分析代码变更，AI 提供审查意见
智能冲突解决：LSP 识别冲突代码，AI 提供合并建议
代码知识库：LSP 提取代码结构，AI 构建知识图谱

10. 实时代码优化

重复代码检测：LSP 查找相似代码，AI 识别并提供重构建议
死代码清理：LSP 分析调用关系，AI 确认无引用代码
依赖优化：LSP 分析依赖树，AI 建议优化方案

