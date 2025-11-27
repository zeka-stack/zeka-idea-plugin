package dev.dong4j.zeka.stack.idea.plugin.common.icons;

import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

/**
 * AI 常用图标工具类
 * <p>
 * 提供 AI 服务提供商相关的图标资源加载和获取功能, 包含不同尺寸的图标资源, 支持根据 AIProviderType 获取对应的图标.
 * <p>
 * 该类通过静态方法加载图标资源, 并提供两个方法用于根据 AIProviderType 获取对应图标, 分别对应常规尺寸和 64 像素尺寸的图标.
 *
 * @author 作者名
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public final class AICommonIcons {
    /**
     * 加载图标
     * <p>
     * 用于加载位于 {@code /icons/} 目录下的图标文件。
     * 路径必须以 {@code /icons/} 开头。
     *
     * @param iconPath 图标文件路径，相对于 resources 根目录（例如："/icons/icon.svg"）
     * @return 加载的图标
     */
    @NotNull
    private static Icon load(@NotNull String iconPath) {
        return IconLoader.getIcon(iconPath, AICommonIcons.class);
    }

    // ========== AI 提供商图标 - 用于下拉列表 (16x16) ==========

    /**
     * OpenAI API 提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_CHATGPT = load("/icons/chatgpt.svg");

    /**
     * 通义千问提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_QWEN = load("/icons/qwen.svg");

    /**
     * 硅基流动提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_SILICONFLOW = load("/icons/siliconflow.svg");

    /**
     * Ollama 提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_OLLAMA = load("/icons/ollama.svg");

    /**
     * LM Studio 提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_LMSTUDIO = load("/icons/lmstudio.svg");

    // ========== AI 提供商图标 - 用于对话框/错误提示框 (64x64) ==========

    /**
     * OpenAI API 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_CHATGPT_64 = load("/icons/chatgpt_64.svg");

    /**
     * 通义千问提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_QWEN_64 = load("/icons/qwen_64.svg");

    /**
     * 硅基流动提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_SILICONFLOW_64 = load("/icons/siliconflow_64.svg");

    /**
     * Ollama 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_OLLAMA_64 = load("/icons/ollama_64.svg");

    /**
     * LM Studio 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_LMSTUDIO_64 = load("/icons/lmstudio_64.svg");

    // ========== 工具方法 ==========

    /**
     * 根据 AIProviderType 获取对应的 16x16 图标
     *
     * @param providerType 提供商类型
     * @return 对应的图标，如果未找到返回 null
     */
    @Nullable
    public static Icon getProviderIcon(@Nullable AIProviderType providerType) {
        if (providerType == null) {
            return null;
        }
        return switch (providerType) {
            case CUSTOM -> PROVIDER_CHATGPT;
            case QIANWEN -> PROVIDER_QWEN;
            case SILICONFLOW -> PROVIDER_SILICONFLOW;
            case OLLAMA -> PROVIDER_OLLAMA;
            case LM_STUDIO -> PROVIDER_LMSTUDIO;
            case MODELSCOPE -> PROVIDER_CHATGPT;
        };
    }

    /**
     * 根据 AIProviderType 获取对应的 64x64 图标
     *
     * @param providerType 提供商类型
     * @return 对应的图标，如果未找到返回 null
     */
    @Nullable
    public static Icon getProviderIcon64(@Nullable AIProviderType providerType) {
        if (providerType == null) {
            return null;
        }
        return switch (providerType) {
            case CUSTOM -> PROVIDER_CHATGPT_64;
            case QIANWEN -> PROVIDER_QWEN_64;
            case SILICONFLOW -> PROVIDER_SILICONFLOW_64;
            case OLLAMA -> PROVIDER_OLLAMA_64;
            case LM_STUDIO -> PROVIDER_LMSTUDIO_64;
            case MODELSCOPE -> PROVIDER_CHATGPT_64;
        };
    }

    /**
     * 私有构造函数, 防止外部实例化
     */
    private AICommonIcons() {
        // 工具类，禁止实例化
    }
}

