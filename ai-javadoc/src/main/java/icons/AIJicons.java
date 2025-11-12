package icons;

import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * AI Javadoc 插件图标管理类
 * <p>
 * 用于集中管理插件中使用的所有图标资源。
 * 图标文件应放置在 {@code src/main/resources/icons/} 目录下。
 * <p>
 * 图标尺寸说明：
 * <ul>
 *   <li>16x16 - Toolbar/Action/Menu/ToolWindow（工具栏、动作、菜单、工具窗口）</li>
 *   <li>24x24 - Notifications（通知图标）</li>
 *   <li>32x32 - Dialog/Settings（对话框、设置面板）</li>
 * </ul>
 *
 * @author dong4j
 * @since 1.0.0
 */
public class AIJicons {
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
        return IconLoader.getIcon(iconPath, AIJicons.class);
    }

    // ========== 16x16 图标 - 用于 Toolbar/Action/Menu/ToolWindow ==========

    /**
     * AI Javadoc 主图标 (16x16)
     * <p>
     * 用于：工具栏按钮、动作图标、菜单项、工具窗口标签
     */
    public static final Icon AIJ_16 = load("/icons/aij_16.svg");

    // ========== 24x24 图标 - 用于 Notifications ==========

    /**
     * AI Javadoc 通知图标 (24x24)
     * <p>
     * 用于：通知弹窗、气球提示
     */
    public static final Icon AIJ_24 = load("/icons/aij_24.svg");

    // ========== 32x32 图标 - 用于 Dialog/Settings ==========

    /**
     * AI Javadoc 对话框图标 (32x32)
     * <p>
     * 用于：设置面板、对话框、大图标显示
     */
    public static final Icon AIJ_32 = load("/icons/aij_32.svg");

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
}
