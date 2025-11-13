package dev.dong4j.zeka.stack.idea.plugin.common.icons;

import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

/**
 * AI Common 图标管理类
 * <p>
 * 用于集中管理 AI 相关的图标资源。
 * 图标文件应放置在 {@code src/main/resources/icons/} 目录下。
 * <p>
 * 图标尺寸说明：
 * <ul>
 *   <li>16x16 - Toolbar/Action/Menu/ToolWindow（工具栏、动作、菜单、工具窗口）</li>
 *   <li>32x32 - Dialog/Settings（对话框、设置面板）</li>
 *   <li>64x64 - Error/Dialog（错误提示框、对话框）</li>
 * </ul>
 *
 * @author dong4j
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
        };
    }

    private AICommonIcons() {
        // 工具类，禁止实例化
    }
}

