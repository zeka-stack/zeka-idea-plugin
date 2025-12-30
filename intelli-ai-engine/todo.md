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

- [ ] 支持 SSE

## 功能增强建议

### 性能监控与分析

- [ ] **模型性能监控**
    - 记录每个模型的响应时间、成功率、错误率
    - 提供性能统计图表和趋势分析
    - 支持性能对比（不同模型、不同时间段）

- [ ] **使用统计与分析**
    - Token 使用量统计（按模型、按时间、按插件）
    - 请求次数统计
    - 成本估算（基于 Token 使用量和模型定价）
    - 导出统计报告（CSV、JSON 格式）

- [ ] **模型对比功能**
    - 同时使用多个模型处理相同请求
    - 对比不同模型的响应质量和速度
    - 生成对比报告

### 批量测试与验证

- [ ] **批量连接测试**
    - 一键测试所有已配置的 AI 提供商
    - 显示测试结果汇总
    - 自动标记不可用的提供商

- [ ] **模型列表自动刷新**
    - 定期自动刷新可用模型列表
    - 检测模型变更并通知用户
    - 支持手动刷新按钮

### 用户体验优化

- [ ] **快速切换提供商**
    - 状态栏显示当前使用的提供商
    - 支持快速切换常用提供商
    - 记住最近使用的提供商

- [ ] **配置导入/导出**
    - 支持导出配置为 JSON 文件
    - 支持从 JSON 文件导入配置
    - 支持配置备份和恢复

- [ ] **配置模板**
    - 提供常用配置模板（开发、测试、生产环境）
    - 支持保存自定义配置模板
    - 一键应用模板配置

### 高级功能

- [ ] **请求重试策略配置**
    - 支持自定义重试次数和间隔
    - 支持指数退避策略配置
    - 支持特定错误码的重试规则

- [ ] **请求限流控制**
    - 支持配置请求速率限制（RPM、TPM）
    - 自动排队和限流
    - 显示限流状态和等待时间

- [ ] **多环境配置管理**
    - 支持开发、测试、生产环境配置隔离
    - 快速切换环境配置
    - 环境配置对比

- [ ] **API Key 轮换提醒**
    - 检测 API Key 过期时间
    - 提前提醒更新 API Key
    - 支持自动测试新 API Key

### 扩展性增强

- [ ] **自定义 Provider 支持**
    - 提供 Provider 开发 SDK
    - 支持第三方自定义 Provider 插件
    - Provider 插件市场

- [ ] **Webhook 集成**
    - 支持配置变更 Webhook
    - 支持使用统计 Webhook
    - 支持错误通知 Webhook

- [ ] **配置同步**
    - 支持配置云端同步（可选）
    - 多设备配置共享
    - 配置版本管理
