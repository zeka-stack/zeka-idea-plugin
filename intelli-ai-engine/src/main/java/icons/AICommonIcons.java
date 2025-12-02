package icons;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

/**
 * AI 通用图标类
 * <p>
 * 提供 AI 服务提供商的图标资源管理, 包含各种 AI 提供商的 16x16 和 64x64 尺寸图标,
 * 并提供根据 AI 提供商类型获取对应图标的方法
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
@SuppressWarnings("DuplicatedCode")
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

    public static final Icon PLUGIN = load("/icons/plugin_16.svg");
    /**
     * 工具图标, 已按比例缩放
     * <p>
     * 该图标用于表示插件相关的工具, 缩放比例为 0.8125
     *
     * @see IconUtil
     */
    public static final Icon TOOL_ICON = PLUGIN;
    /**
     * OpenAI API 提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_CHATGPT = load("/icons/chatgpt_16.svg");

    /**
     * 通义千问提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_QWEN = load("/icons/qwen_16.svg");

    /**
     * 硅基流动提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_SILICONFLOW = load("/icons/siliconflow_16.svg");

    /**
     * Ollama 提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_OLLAMA = load("/icons/ollama_16.svg");

    /**
     * LM Studio 提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_LMSTUDIO = load("/icons/lmstudio_16.svg");

    /** 模型 scope 图标资源 */
    public static final Icon PROVIDER_MODELSCOPE = load("/icons/modelscope_16.svg");

    /**
     * IFlow 提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_IFLOW = load("/icons/iflow_16.svg");

    /**
     * 智谱AI 提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_ZHIPU = load("/icons/chatglm_16.svg");

    // ========== AI 提供商图标 - 用于对话框/错误提示框 (64x64) ==========

    /**
     * OpenAI API 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_CHATGPT_64 = load("/icons/chatgpt_64.svg");
    /** 模型 scope 64x64 像素图标资源 */
    public static final Icon PROVIDER_MODELSCOPE_64 = load("/icons/modelscope_64.svg");

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

    /**
     * IFlow 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_IFLOW_64 = load("/icons/iflow_64.svg");

    /**
     * 智谱AI 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_ZHIPU_64 = load("/icons/chatglm_64.svg");

    // ========== 工具方法 ==========

    /**
     * 根据 AIProviderType 获取对应的图标（已缩放为 13x13）
     * <p>
     * 该方法返回的图标已从 16x16 缩放到 13x13，适合在状态栏、下拉列表和表格中使用。
     * 缩放比例：13/16 = 0.8125
     *
     * @param providerType 提供商类型
     * @return 对应的图标（已缩放），如果未找到返回 null
     */
    @Nullable
    public static Icon getProviderIcon(@Nullable AIProviderType providerType) {
        if (providerType == null) {
            return null;
        }
        Icon icon = switch (providerType) {
            case CUSTOM -> PROVIDER_CHATGPT;
            case QIANWEN -> PROVIDER_QWEN;
            case SILICONFLOW -> PROVIDER_SILICONFLOW;
            case OLLAMA -> PROVIDER_OLLAMA;
            case LM_STUDIO -> PROVIDER_LMSTUDIO;
            case MODELSCOPE -> PROVIDER_MODELSCOPE;
            case IFLOW -> PROVIDER_IFLOW;
            case ZHIPU -> PROVIDER_ZHIPU;
        };
        // 将图标从 16x16 缩放到 13x13，适合状态栏显示
        // 状态栏图标通常使用 13x13 尺寸
        return IconUtil.scale(icon, null, 0.8125f);
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
            case MODELSCOPE -> PROVIDER_MODELSCOPE_64;
            case IFLOW -> PROVIDER_IFLOW_64;
            case ZHIPU -> PROVIDER_ZHIPU_64;
        };
    }

    /**
     * 私有构造函数, 防止外部实例化
     */
    private AICommonIcons() {
        // 工具类，禁止实例化
    }
}

