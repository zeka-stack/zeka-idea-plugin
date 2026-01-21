package icons;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

/**
 * 用户服务类
 * <p> 提供用户相关的业务逻辑处理, 包括用户的查询, 创建, 更新和删除等操作
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.10.24
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
     * @param iconPath 图标文件路径, 相对于 resources 根目录(例如:"/icons/icon.svg")
     * @return 加载的图标
     */
    @NotNull
    private static Icon load(@NotNull String iconPath) {
        return IconLoader.getIcon(iconPath, AICommonIcons.class);
    }

    // ========== AI 提供商图标 - 用于下拉列表 (16x16) ==========

    /** 插件图标资源, 尺寸为 16x16, 用于界面中标识插件功能. */
    public static final Icon PLUGIN = load("/icons/plugin_16.svg");
    /**
     * 工具图标
     */
    public static final Icon TOOL_ICON = PLUGIN;
    /** ChatGPT 提供商图标 (16x16 像素), 用于设置页面 AI 供应商下拉列表及状态栏 */
    public static final Icon PROVIDER_CHATGPT = load("/icons/chatgpt_16.svg");

    /**
     * Anthropic 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_ANTHROPIC = load("/icons/anthropic.svg");

    /**
     * Anthropic 提供商图标 (32x32)
     * <p> 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_ANTHROPIC_32 = load("/icons/anthropic_32.svg");

    /**
     * Gemini 提供商图标 (16x16 像素)
     * <p>
     * 用于设置页面 AI 供应商下拉列表, 状态栏和错误提示框, 对话框
     */
    public static final Icon PROVIDER_GEMINI = load("/icons/gemini.svg");

    /**
     * Gemini 提供商图标 (32x32 像素)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_GEMINI_32 = load("/icons/gemini_32.svg");

    /**
     * Qwen 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_QWEN = load("/icons/qwen_16.svg");

    /**
     * NVIDIA 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_NVIDIA = load("/icons/nvidia.svg");

    /**
     * NVIDIA 提供商图标 (32x32 像素)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_NVIDIA_32 = load("/icons/nvidia_32.svg");

    /**
     * HuggingFace 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_HUGGINGFACE = load("/icons/huggingface.svg");

    /**
     * OpenRouter 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_OPENROUTER = load("/icons/openrouter.svg");

    /**
     * OpenRouter 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_OPENROUTER_32 = load("/icons/openrouter_32.svg");

    /**
     * Cloudflare 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_CLOUDFLARE = load("/icons/cloudflare.svg");

    /**
     * Cloudflare 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_CLOUDFLARE_32 = load("/icons/cloudflare_32.svg");

    /**
     * HuggingFace 提供商图标 (32x32)
     * <p>
     * 用于显示在需要时作为 AI 提供商图标, 尤其是在列表, 下拉选项或特定场景中.
     *
     */
    public static final Icon PROVIDER_HUGGINGFACE_32 = load("/icons/huggingface_32.svg");

    /**
     * GitHub 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_GITHUB = load("/icons/github_16.svg");

    /**
     * GitHub 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_GITHUB_32 = load("/icons/github_32.svg");

    /**
     * Azure OpenAI 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_AZUREAI = load("/icons/azureai.svg");

    /**
     * Azure OpenAI 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_AZUREAI_32 = load("/icons/azureai_32.svg");

    /**
     * Amazon Bedrock 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_BEDROCK = load("/icons/bedrock.svg");

    /**
     * Amazon Bedrock 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_BEDROCK_32 = load("/icons/bedrock_32.svg");

    /**
     * Mistral 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_MISTRAL = load("/icons/mistral.svg");

    /**
     * DeepSeek 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_DEEPSEEK = load("/icons/deepseek_16.svg");

    /**
     * 豆包 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_DOUBAO = load("/icons/doubao_16.svg");

    /**
     * Grok 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_GROK = load("/icons/grok_16.svg");

    /**
     * 混元 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_HUNYUAN = load("/icons/hunyuan_16.svg");

    /**
     * Moonshot 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_MOONSHOT = load("/icons/moonshot_16.svg");

    /**
     * Mistral 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_MISTRAL_32 = load("/icons/mistral_32.svg");

    /**
     * DeepSeek 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_DEEPSEEK_32 = load("/icons/deepseek_32.svg");

    /**
     * 豆包 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_DOUBAO_32 = load("/icons/doubao_32.svg");

    /**
     * Grok 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_GROK_32 = load("/icons/grok_32.svg");

    /**
     * 混元 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_HUNYUAN_32 = load("/icons/hunyuan_32.svg");

    /**
     * Moonshot 提供商图标 (32x32)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_MOONSHOT_32 = load("/icons/moonshot_32.svg");

    /**
     * 提供商图标, 尺寸为 16x16, 用于标识插件功能和供应商类型.
     * <p> 该图标用于图形界面中表示 AI 提供商, 以及在不同场景下用于不同功能.
     *
     * @see AICommonIcons
     * @since 1.0.0
     */
    public static final Icon PROVIDER_SILICONFLOW = load("/icons/siliconflow_16.svg");

    /**
     * Ollama 提供商图标 (16x16)
     * <p>
     * 用于：设置页面 AI 供应商下拉列表、状态栏
     */
    public static final Icon PROVIDER_OLLAMA = load("/icons/ollama_16.svg");

    /**
     * LM Studio 提供商图标 (16x16 像素)
     * <p>
     * 用于设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_LMSTUDIO = load("/icons/lmstudio_16.svg");

    /** 模型 scope 图标资源 */
    public static final Icon PROVIDER_MODELSCOPE = load("/icons/modelscope_16.svg");

    /**
     * IFlow 提供商图标 (16x16)
     * <p> 用于: 设置页面 AI 供应商下拉列表, 状态栏 </p>
     */
    public static final Icon PROVIDER_IFLOW = load("/icons/iflow_16.svg");

    /**
     * 智谱 AI 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_ZHIPU = load("/icons/chatglm_16.svg");

    /**
     * Z.AI 提供商图标 (16x16)
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏
     */
    public static final Icon PROVIDER_ZAI = load("/icons/zai.svg");

    /**
     * Z.AI 提供商图标 (32x32)
     * <p> 用于: 设置页面 AI 供应商下拉列表, 状态栏 </p>
     */
    public static final Icon PROVIDER_ZAI_32 = load("/icons/zai_32.svg");

    // ========== AI 提供商图标 - 用于对话框/错误提示框 (64x64) ==========

    /**
     * chatgpt_64.svg 图标资源, 尺寸为 64x64 像素
     * <p> 用于错误提示框, 对话框 </p>
     *
     * @since 1.0.0
     */
    public static final Icon PROVIDER_CHATGPT_64 = load("/icons/chatgpt_64.svg");

    /**
     * Anthropic 提供商图标 (64x64)
     * <p> 用于: 错误提示框, 对话框 </p>
     */
    public static final Icon PROVIDER_ANTHROPIC_64 = load("/icons/anthropic_64.svg");
    /** modelscope_64 图标资源, 尺寸为 64x64 */
    public static final Icon PROVIDER_MODELSCOPE_64 = load("/icons/modelscope_64.svg");

    /**
     * Qwen 64x64 像素 AI 提供商图标
     * <p>
     * 用于: 设置页面 AI 供应商下拉列表, 状态栏, 错误提示框, 对话框
     *
     * @see AICommonIcons
     */
    public static final Icon PROVIDER_QWEN_64 = load("/icons/qwen_64.svg");

    /**
     * Gemini 提供商图标 (64x64)
     * <p>
     * 用于: 错误提示框, 对话框
     */
    public static final Icon PROVIDER_GEMINI_64 = load("/icons/gemini_64.svg");

    /**
     * NVIDIA 提供商图标 (64x64)
     * <p>
     * 用于: 错误提示框, 对话框
     */
    public static final Icon PROVIDER_NVIDIA_64 = load("/icons/nvidia_64.svg");

    /**
     * HuggingFace 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_HUGGINGFACE_64 = load("/icons/huggingface_64.svg");

    /**
     * GitHub 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_GITHUB_64 = load("/icons/github_64.svg");

    /**
     * Azure OpenAI 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_AZUREAI_64 = load("/icons/azureai_64.svg");

    /**
     * Amazon Bedrock 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_BEDROCK_64 = load("/icons/bedrock_64.svg");

    /**
     * Mistral 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_MISTRAL_64 = load("/icons/mistral_64.svg");

    /**
     * DeepSeek 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_DEEPSEEK_64 = load("/icons/deepseek_64.svg");

    /**
     * 豆包 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_DOUBAO_64 = load("/icons/doubao_64.svg");

    /**
     * Grok 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_GROK_64 = load("/icons/grok_64.svg");

    /**
     * 混元 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_HUNYUAN_64 = load("/icons/hunyuan_64.svg");

    /**
     * Moonshot 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_MOONSHOT_64 = load("/icons/moonshot_64.svg");

    /**
     * OpenRouter 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_OPENROUTER_64 = load("/icons/openrouter_64.svg");

    /**
     * Cloudflare 提供商图标 (64x64)
     * <p>
     * 用于：错误提示框、对话框
     */
    public static final Icon PROVIDER_CLOUDFLARE_64 = load("/icons/cloudflare_64.svg");

    /** 模型 scope 64x64 像素图标资源 */
    public static final Icon PROVIDER_SILICONFLOW_64 = load("/icons/siliconflow_64.svg");

    /**
     * 64x64 像素的 Ollama 提供商图标资源.
     * <p>
     * 用于错误提示框和对话框.
     */
    public static final Icon PROVIDER_OLLAMA_64 = load("/icons/ollama_64.svg");

    /**
     * LM Studio 提供商图标 (64x64)
     * <p>
     * 用于: 错误提示框, 对话框
     */
    public static final Icon PROVIDER_LMSTUDIO_64 = load("/icons/lmstudio_64.svg");

    /**
     * 根据 AIProviderType 获取对应的 64x64 图标
     * <p>
     * 此方法用于返回指定类型提供商对应的 64x64 图标资源.
     *
     */
    public static final Icon PROVIDER_IFLOW_64 = load("/icons/iflow_64.svg");

    /**
     * 智谱 AI 提供商图标 (64x64)
     * <p>
     * 用于: 错误提示框, 对话框
     */
    public static final Icon PROVIDER_ZHIPU_64 = load("/icons/chatglm_64.svg");

    /**
     * Z.AI 提供商图标 (64x64)
     * <p> 用于: 错误提示框, 对话框 </p>
     */
    public static final Icon PROVIDER_ZAI_64 = load("/icons/zai_64.svg");

    // ========== 支付方式图标 ==========

    /**
     * 微信支付图标
     * <p> 用于支持对话框中的支付方式显示 </p>
     *
     * @see AICommonIcons#load(String) 加载图标
     */
    public static final Icon WECHAT_PAY = load("/images/wechat.webp");

    /**
     * 支付宝图标
     * <p> 用于支持对话框中的支付方式显示 </p>
     */
    public static final Icon ALIPAY = load("/images/alipay.webp");

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
            case OPENAI, CODEX -> PROVIDER_CHATGPT;
            case OPENROUTER -> PROVIDER_OPENROUTER;
            case CLOUDFLARE -> PROVIDER_CLOUDFLARE;
            case BEDROCK -> PROVIDER_BEDROCK;
            case AZURE -> PROVIDER_AZUREAI;
            case MISTRAL -> PROVIDER_MISTRAL;
            case GITHUB_MODELS -> PROVIDER_GITHUB;
            case ANTHROPIC -> PROVIDER_ANTHROPIC;
            case GEMINI -> PROVIDER_GEMINI;
            case NVIDIA -> PROVIDER_NVIDIA;
            case HUGGINGFACE -> PROVIDER_HUGGINGFACE;
            case DEEPSEEK, DEEPSEEK_ANTHROPIC -> PROVIDER_DEEPSEEK;
            case DOUBAO, DOUBAO_ANTHROPIC -> PROVIDER_DOUBAO;
            case GROK -> PROVIDER_GROK;
            case HUNYUAN, HUNYUAN_ANTHROPIC -> PROVIDER_HUNYUAN;
            case MOONSHOT, MOONSHOT_ANTHROPIC -> PROVIDER_MOONSHOT;
            case ZHIPU_ANTHROPIC, ZHIPU -> PROVIDER_ZHIPU;
            case QIANWEN -> PROVIDER_QWEN;
            case SILICONFLOW -> PROVIDER_SILICONFLOW;
            case OLLAMA -> PROVIDER_OLLAMA;
            case LM_STUDIO -> PROVIDER_LMSTUDIO;
            case MODELSCOPE, MODELSCOPE_ANTHROPIC -> PROVIDER_MODELSCOPE;
            case IFLOW -> PROVIDER_IFLOW;
            case ZAI, ZAI_ANTHROPIC -> PROVIDER_ZAI;
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
            case OPENAI, CODEX -> PROVIDER_CHATGPT_64;
            case OPENROUTER -> PROVIDER_OPENROUTER_64;
            case CLOUDFLARE -> PROVIDER_CLOUDFLARE_64;
            case BEDROCK -> PROVIDER_BEDROCK_64;
            case AZURE -> PROVIDER_AZUREAI_64;
            case MISTRAL -> PROVIDER_MISTRAL_64;
            case GITHUB_MODELS -> PROVIDER_GITHUB_64;
            case ANTHROPIC -> PROVIDER_ANTHROPIC_64;
            case GEMINI -> PROVIDER_GEMINI_64;
            case NVIDIA -> PROVIDER_NVIDIA_64;
            case HUGGINGFACE -> PROVIDER_HUGGINGFACE_64;
            case DEEPSEEK, DEEPSEEK_ANTHROPIC -> PROVIDER_DEEPSEEK_64;
            case DOUBAO, DOUBAO_ANTHROPIC -> PROVIDER_DOUBAO_64;
            case GROK -> PROVIDER_GROK_64;
            case HUNYUAN, HUNYUAN_ANTHROPIC -> PROVIDER_HUNYUAN_64;
            case MOONSHOT, MOONSHOT_ANTHROPIC -> PROVIDER_MOONSHOT_64;
            case ZHIPU_ANTHROPIC, ZHIPU -> PROVIDER_ZHIPU_64;
            case QIANWEN -> PROVIDER_QWEN_64;
            case SILICONFLOW -> PROVIDER_SILICONFLOW_64;
            case OLLAMA -> PROVIDER_OLLAMA_64;
            case LM_STUDIO -> PROVIDER_LMSTUDIO_64;
            case MODELSCOPE, MODELSCOPE_ANTHROPIC -> PROVIDER_MODELSCOPE_64;
            case IFLOW -> PROVIDER_IFLOW_64;
            case ZAI, ZAI_ANTHROPIC -> PROVIDER_ZAI_64;
        };
    }

    /**
     * 私有构造函数
     * <p> 防止外部实例化 </p>
     */
    private AICommonIcons() {
        // 工具类，禁止实例化
    }
}
