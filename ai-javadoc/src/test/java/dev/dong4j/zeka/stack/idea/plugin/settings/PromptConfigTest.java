package dev.dong4j.zeka.stack.idea.plugin.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.plugin.settings.ui.JavaDocSettingsPanel;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Prompt 配置测试类
 * <p>
 * 用于验证 JavaDoc 提示词模板的保存、加载、重置以及空值处理功能的测试类。
 * 包括对系统提示词、类提示词、方法提示词、字段提示词和测试提示词的测试。
 * 确保 UI 交互与配置状态的同步正确性，以及默认模板的有效性。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@Slf4j
public class PromptConfigTest {

    /** 设置面板，用于配置 JavaDoc 生成相关参数 */
    private JavaDocSettingsPanel settingsPanel;
    /** 原始设置状态，用于保存未修改前的设置信息 */
    private SettingsState originalSettings;

    /**
     * 初始化测试环境，设置原始配置和配置面板
     * <p>
     * 在每次测试前创建新的配置实例，并初始化配置面板，加载当前配置到面板中
     *
     * @since 1.0
     */
    @BeforeEach
    public void setUp() {
        // 创建新的设置实例
        originalSettings = new SettingsState();

        // 创建设置面板
        settingsPanel = new JavaDocSettingsPanel();

        // 加载当前设置到面板
        settingsPanel.loadSettings(originalSettings);
    }

    /**
     * 测试 Prompt 模板的修改检测功能
     * <p>
     * 测试场景：用户修改了系统提示词后，验证系统是否能够正确检测到修改
     * 预期结果：修改前后的系统提示词应不一致，且应与用户输入的修改内容一致
     * <p>
     * 说明：测试通过模拟用户输入修改内容，并对比获取到的设置值，验证修改检测机制是否正常工作
     */
    @Test
    public void testPromptModificationDetection() {
        // 获取当前设置
        SettingsState currentSettings = settingsPanel.getSettings();

        // 修改系统提示词
        String originalSystemPrompt = currentSettings.systemPromptTemplate;
        String modifiedSystemPrompt = "修改后的系统提示词";

        // 模拟用户修改
        settingsPanel.systemPromptTextArea.setText(modifiedSystemPrompt);

        // 获取修改后的设置
        SettingsState modifiedSettings = settingsPanel.getSettings();

        // 验证修改被检测到
        assertNotEquals(originalSystemPrompt, modifiedSettings.systemPromptTemplate);
        assertEquals(modifiedSystemPrompt, modifiedSettings.systemPromptTemplate);
    }

    /**
     * 测试 Prompt 模板的重置功能
     * <p>
     * 测试场景：用户修改了系统提示词后，调用重置方法将其恢复为默认值
     * 预期结果：重置后文本应与默认提示词一致，且与修改前的自定义提示词不同
     * <p>
     * 该测试验证了重置功能的正确性，确保系统提示词能够正确恢复到默认状态
     */
    @Test
    public void testPromptReset() {
        // 修改系统提示词
        String customPrompt = "自定义的系统提示词";
        settingsPanel.systemPromptTextArea.setText(customPrompt);

        // 验证修改生效
        assertEquals(customPrompt, settingsPanel.systemPromptTextArea.getText());

        // 重置为默认值
        settingsPanel.resetPromptToDefault("system", settingsPanel.systemPromptTextArea);

        // 验证重置成功
        String defaultPrompt = SettingsState.getDefaultSystemPromptTemplate();
        assertEquals(defaultPrompt, settingsPanel.systemPromptTextArea.getText());
        assertNotEquals(customPrompt, settingsPanel.systemPromptTextArea.getText());
    }

    /**
     * 测试所有 Prompt 模板的重置功能
     * <p>
     * 测试场景：修改所有提示词后，调用重置方法将提示词恢复为默认值
     * 预期结果：所有提示词文本应与对应的默认模板一致
     * <p>
     * 该测试验证了 {@link SettingsPanel#resetPromptToDefault(String, javax.swing.JTextField)} 方法的正确性，
     * 确保每个提示词字段在调用重置方法后能够正确恢复为系统预设的默认值。
     */
    @Test
    public void testAllPromptReset() {
        // 修改所有提示词
        String customSystemPrompt = "自定义系统提示词";
        String customClassPrompt = "自定义类提示词";
        String customMethodPrompt = "自定义方法提示词";
        String customFieldPrompt = "自定义字段提示词";
        String customTestPrompt = "自定义测试提示词";

        settingsPanel.systemPromptTextArea.setText(customSystemPrompt);
        settingsPanel.classPromptTextArea.setText(customClassPrompt);
        settingsPanel.methodPromptTextArea.setText(customMethodPrompt);
        settingsPanel.fieldPromptTextArea.setText(customFieldPrompt);
        settingsPanel.testPromptTextArea.setText(customTestPrompt);

        // 重置所有提示词
        settingsPanel.resetPromptToDefault("system", settingsPanel.systemPromptTextArea);
        settingsPanel.resetPromptToDefault("class", settingsPanel.classPromptTextArea);
        settingsPanel.resetPromptToDefault("method", settingsPanel.methodPromptTextArea);
        settingsPanel.resetPromptToDefault("field", settingsPanel.fieldPromptTextArea);
        settingsPanel.resetPromptToDefault("test", settingsPanel.testPromptTextArea);

        // 验证所有提示词都重置为默认值
        assertEquals(SettingsState.getDefaultSystemPromptTemplate(), settingsPanel.systemPromptTextArea.getText());
        assertEquals(SettingsState.getDefaultClassPromptTemplate(), settingsPanel.classPromptTextArea.getText());
        assertEquals(SettingsState.getDefaultMethodPromptTemplate(), settingsPanel.methodPromptTextArea.getText());
        assertEquals(SettingsState.getDefaultFieldPromptTemplate(), settingsPanel.fieldPromptTextArea.getText());
        assertEquals(SettingsState.getDefaultTestPromptTemplate(), settingsPanel.testPromptTextArea.getText());
    }

    /**
     * 测试 Prompt 模板的保存和加载功能
     * <p>
     * 测试场景：用户修改系统提示词和类提示词后，验证设置是否被正确保存，并通过新面板加载验证数据是否准确还原
     * 预期结果：保存后的设置与加载后的面板内容应保持一致
     */
    @Test
    public void testPromptSaveAndLoad() {
        // 修改提示词
        String customSystemPrompt = "测试系统提示词";
        String customClassPrompt = "测试类提示词";

        settingsPanel.systemPromptTextArea.setText(customSystemPrompt);
        settingsPanel.classPromptTextArea.setText(customClassPrompt);

        // 获取修改后的设置
        SettingsState modifiedSettings = settingsPanel.getSettings();

        // 验证设置被正确保存
        assertEquals(customSystemPrompt, modifiedSettings.systemPromptTemplate);
        assertEquals(customClassPrompt, modifiedSettings.classPromptTemplate);

        // 创建新的面板并加载设置
        JavaDocSettingsPanel newPanel = new JavaDocSettingsPanel();
        newPanel.loadSettings(modifiedSettings);

        // 验证设置被正确加载
        assertEquals(customSystemPrompt, newPanel.systemPromptTextArea.getText());
        assertEquals(customClassPrompt, newPanel.classPromptTextArea.getText());
    }

    /**
     * 测试空提示词的处理
     * <p>
     * 测试场景：当系统提示词和类提示词为空或仅包含空格时
     * 预期结果：设置对象中的提示词字段应被正确 trim 处理为为空字符串
     * <p>
     * 注意：此测试需要确保 settingsPanel 的文本区域和 getSettings 方法能正确读取和处理输入
     */
    @Test
    public void testEmptyPromptHandling() {
        // 设置空提示词
        settingsPanel.systemPromptTextArea.setText("");
        settingsPanel.classPromptTextArea.setText("   "); // 只有空格

        // 获取设置
        SettingsState settings = settingsPanel.getSettings();

        // 验证空提示词被正确处理（trim 后为空）
        assertEquals("", settings.systemPromptTemplate);
        assertEquals("", settings.classPromptTemplate);
    }

    /**
     * 测试默认提示词模板的有效性
     * <p>
     * 测试场景：验证系统默认的各类提示词模板是否已正确初始化
     * 预期结果：所有默认提示词模板对象不为空且内容不为空字符串
     * <p>
     * 说明：该测试确保应用在未配置任何自定义提示词时，能够提供有效的默认模板
     */
    @Test
    public void testDefaultPromptTemplates() {
        // 验证默认提示词不为空
        assertNotNull(SettingsState.getDefaultSystemPromptTemplate());
        assertNotNull(SettingsState.getDefaultClassPromptTemplate());
        assertNotNull(SettingsState.getDefaultMethodPromptTemplate());
        assertNotNull(SettingsState.getDefaultFieldPromptTemplate());
        assertNotNull(SettingsState.getDefaultTestPromptTemplate());

        // 验证默认提示词不为空字符串
        assertFalse(SettingsState.getDefaultSystemPromptTemplate().trim().isEmpty());
        assertFalse(SettingsState.getDefaultClassPromptTemplate().trim().isEmpty());
        assertFalse(SettingsState.getDefaultMethodPromptTemplate().trim().isEmpty());
        assertFalse(SettingsState.getDefaultFieldPromptTemplate().trim().isEmpty());
        assertFalse(SettingsState.getDefaultTestPromptTemplate().trim().isEmpty());
    }
}
