## 2025.11.24

1. 将模型参数与添加到可用列表的模型关联起来, 而不是共用同一套模型参数
2. Ollama LMStudio 不强制限制是否输入 api key

## 2025.11.27

1. 超时时间单位改成秒
2. token 最大长度单位改成 K
3. 模型下拉列表支持搜索筛选(忽略大小写)
4. 添加 modelscope 服务商

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