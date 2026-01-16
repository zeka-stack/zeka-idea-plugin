package dev.dong4j.zeka.stack.api.plugin.model.controller;

import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import dev.dong4j.zeka.stack.api.plugin.model.dto.ModelResponse;
import dev.dong4j.zeka.stack.api.plugin.model.service.ZhipuModelService;
import dev.dong4j.zeka.starter.rest.annotation.RestControllerWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 模型控制器类
 * <p> 用于处理与智谱 AI 模型相关的查询请求, 通过 RESTful 接口暴露模型列表信息. 该控制器不负责请求处理逻辑本身, 而是专注于模型数据的检索与封装, 确保业务逻辑与基础设施解耦.</p>
 * <p> 控制器路径为 <code>/plugin/v1/models</code>, 提供 <code>GET /zhipu</code> 接口用于获取智谱 AI 可用模型列表. 支持通过 <code>apiKey</code> 参数指定访问密钥,
 * 若未提供则使用默认配置.</p>
 * <p> 返回结构为 <code>ModelResponse</code>, 包含提供者名称, 模型列表及总数, 便于前端或下游服务消费.</p>
 * <p> 本类为内部使用组件, 不对外暴露请求处理细节, 符合面向对象设计原则, 专注于模型数据的封装与响应.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Slf4j
@RestControllerWrapper("/plugin/v1/models")
@RequiredArgsConstructor
public class ModelController {

    /** 智谱 AI 模型服务, 用于获取可用模型列表 */
    private final ZhipuModelService zhipuModelService;

    /**
     * 获取智谱 AI 可用模型列表
     * <p> 通过 HTTP GET 请求获取智谱 AI 平台支持的模型列表, 支持传入 API Key 以获取个性化模型列表.
     * 若未提供 API Key, 则返回默认公开模型列表.
     *
     * @return 包含模型提供商名称, 模型列表及总数的响应对象
     */
    @GetMapping("/zhipu")
    public ModelResponse getZhipuModels() {

        List<String> models = zhipuModelService.getAvailableModels();

        return ModelResponse.builder()
            .provider("zhipu")
            .models(models)
            .total(models.size())
            .build();
    }
}
