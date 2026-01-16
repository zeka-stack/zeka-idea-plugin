package dev.dong4j.zeka.stack.api.plugin.model.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.api.plugin.model.config.ZhipuProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 智谱 AI 模型服务类
 * <p> 提供智谱 AI 平台可用模型的列表查询功能, 用于在系统内部获取当前支持的模型名称集合. 该服务不处理外部请求, 仅作为内部服务层组件, 用于模型枚举和配置管理. 通过注入的 {@code ZhipuProperties} 配置属性,
 * 可适配不同环境下的模型可用性判断. 服务层设计遵循面向对象原则, 避免与基础设施层耦合.</p>
 * <p> 当前硬编码返回的模型列表包括:<pre>{@code
 * glm-4.6
 * glm-4.5
 * glm-4.5-air
 * glm-4.5-x
 * glm-4.5-airx
 * glm-4.5-flash
 * glm-4-plus
 * glm-4-air-250414
 * glm-4-airx
 * glm-4-flashx
 * glm-4-flashx-250414
 * }</pre>, 实际使用时可通过配置动态调整.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZhipuModelService {

    /** 智谱 AI 配置属性, 用于读取服务所需的配置信息 */
    private final ZhipuProperties zhipuProperties;

    /**
     * 获取智谱 AI 可用模型列表
     * <p>
     * 从配置文件中读取默认模型列表, 后续可扩展为从智谱 AI API 动态获取.
     * <a href="https://docs.bigmodel.cn/api-reference/%E6%A8%A1%E5%9E%8B-api/%E5%AF%B9%E8%AF%9D%E8%A1%A5%E5%85%A8#%E6%96%87%E6%9C%AC%E6%A8%A1%E5%9E%8B"> 智谱 AI 模型 API 文档 </a>
     *
     * @return 包含多个智谱 AI 模型名称的列表, 从配置文件读取
     */
    public List<String> getAvailableModels() {

        // 从配置中读取默认模型列表
        List<String> models = new ArrayList<>(zhipuProperties.getDefaultModels());

        // 如果配置为空，使用硬编码的默认列表作为后备
        if (models.isEmpty()) {
            log.warn("配置中的模型列表为空，使用硬编码的默认列表");
            models.add("glm-4.7");
        }

        log.info("成功获取 {} 个智谱 AI 模型", models.size());
        return models;
    }

}
