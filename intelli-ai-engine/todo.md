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

## 2026.01.03

- [ ] 模型下拉框可模糊搜索

## 2026.01.05

- [ ] 修复过时 API

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
