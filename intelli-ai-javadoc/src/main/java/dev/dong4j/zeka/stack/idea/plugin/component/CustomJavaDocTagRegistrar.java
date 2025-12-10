package dev.dong4j.zeka.stack.idea.plugin.component;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.javaDoc.JavadocDeclarationInspection;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * 自定义 Javadoc 标签注册器
 * <p>
 * 该类实现了 ProjectActivity 接口, 用于在 IDE 启动时同步自定义的 Javadoc 标签配置.
 * 通过反射机制操作 JavadocDeclarationInspection 检查工具, 动态添加或移除自定义的 Javadoc 标签,
 * 使 IDE 能够识别和验证项目中使用的自定义 Javadoc 标签. 主要功能包括: 同步配置的自定义标签,
 * 解析和管理标签字符串, 以及通过反射操作检查工具的内部字段.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class CustomJavaDocTagRegistrar implements ProjectActivity {

    /**
     * 在项目启动时运行，注册自定义的 Javadoc 标签
     *
     * @param project 启动的项目
     * @param continuation Kotlin 协程 continuation
     * @return Unit 对象
     */
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 在写操作中执行标签注册
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                syncCustomTags(project);
            });
        });
        return Unit.INSTANCE;
    }

    /**
     * 同步自定义标签到 JavadocDeclarationInspection
     * <p>
     * 该方法会：
     * <ol>
     *   <li>读取配置中的自定义标签列表</li>
     *   <li>读取当前已注册的标签</li>
     *   <li>计算需要添加和删除的标签</li>
     *   <li>执行同步操作</li>
     * </ol>
     *
     * @param project 项目对象
     */
    public static void syncCustomTags(@NotNull Project project) {
        try {
            // 获取项目的检查配置管理器
            ProjectInspectionProfileManager profileManager =
                ProjectInspectionProfileManager.getInstance(project);

            // 获取当前的检查配置
            InspectionProfile profile = profileManager.getCurrentProfile();

            // 获取 JavadocDeclarationInspection 工具
            InspectionToolWrapper<?, ?> toolWrapper =
                profile.getInspectionTool("JavadocDeclaration", project);

            if (toolWrapper != null) {
                // 获取实际的检查工具实例
                Object tool = toolWrapper.getTool();

                // 检查是否是 JavadocDeclarationInspection 类型
                if (tool instanceof JavadocDeclarationInspection inspection) {
                    // 执行标签同步
                    performTagSync(inspection);

                    // 通知配置已更改
                    profileManager.fireProfileChanged();

                    // 重启代码分析，使更改立即生效
                    DaemonCodeAnalyzer.getInstance(project).restart();
                }
            }
        } catch (Exception e) {
            // 静默处理异常，避免影响 IDE 启动
            // 可以在详细日志模式下记录错误
        }
    }

    /**
     * 执行标签同步操作
     * <p>
     * 核心逻辑：
     * <ol>
     *   <li>读取配置中的标签列表（规范化处理）</li>
     *   <li>读取当前已注册的标签</li>
     *   <li>计算差异：需要添加和删除的标签</li>
     *   <li>执行删除操作（先删除，避免重复）</li>
     *   <li>执行添加操作（添加新标签）</li>
     * </ol>
     *
     * @param inspection Javadoc 检查工具实例
     */
    private static void performTagSync(JavadocDeclarationInspection inspection) {
        try {
            // 1. 读取配置中的标签列表（规范化处理）
            SettingsState settings = SettingsState.getInstance();
            List<String> configuredTags = settings.getNormalizedCustomJavaDocTags();

            // 2. 读取当前已注册的标签
            String currentTagsString = getCurrentAdditionalTags(inspection);
            List<String> currentTags = parseTagsString(currentTagsString);

            // 3. 计算差异
            Set<String> configuredTagsSet = new HashSet<>(configuredTags);
            Set<String> currentTagsSet = new HashSet<>(currentTags);

            // 需要删除的标签：已注册但配置中没有的
            List<String> tagsToRemove = currentTags.stream()
                .filter(tag -> !configuredTagsSet.contains(tag))
                .collect(Collectors.toList());

            // 需要添加的标签：配置中有但未注册的
            List<String> tagsToAdd = configuredTags.stream()
                .filter(tag -> !currentTagsSet.contains(tag))
                .collect(Collectors.toList());

            // 4. 执行删除操作
            if (!tagsToRemove.isEmpty()) {
                removeTags(inspection, tagsToRemove);
            }

            // 5. 执行添加操作
            if (!tagsToAdd.isEmpty()) {
                addTags(inspection, tagsToAdd);
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }

    /**
     * 获取当前已注册的标签字符串
     *
     * @param inspection Javadoc 检查工具实例
     * @return 当前标签字符串，如果获取失败返回空字符串
     */
    private static String getCurrentAdditionalTags(JavadocDeclarationInspection inspection) {
        try {
            java.lang.reflect.Field additionalTagsField =
                JavadocDeclarationInspection.class.getDeclaredField("ADDITIONAL_TAGS");
            additionalTagsField.setAccessible(true);
            Object value = additionalTagsField.get(inspection);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 解析标签字符串为列表
     * <p>
     * 标签字符串格式：逗号分隔，如 "date,email,xxx"
     *
     * @param tagsString 标签字符串
     * @return 标签列表（已规范化：去空、转小写、去重）
     */
    private static List<String> parseTagsString(String tagsString) {
        if (tagsString == null || tagsString.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(tagsString.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toLowerCase)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 设置标签字符串
     *
     * @param inspection Javadoc 检查工具实例
     * @param tagsString 新的标签字符串
     */
    private static void setAdditionalTags(JavadocDeclarationInspection inspection,
                                          String tagsString) {
        try {
            java.lang.reflect.Field additionalTagsField =
                JavadocDeclarationInspection.class.getDeclaredField("ADDITIONAL_TAGS");
            additionalTagsField.setAccessible(true);
            additionalTagsField.set(inspection, tagsString);
        } catch (Exception e) {
            // 如果直接设置字段失败，尝试使用反射调用方法
            // 注意：这里无法直接删除，只能通过重新设置整个字符串来实现
        }
    }

    /**
     * 添加标签到 JavadocDeclarationInspection
     *
     * @param inspection Javadoc 检查工具实例
     * @param tagsToAdd  要添加的标签列表
     */
    private static void addTags(JavadocDeclarationInspection inspection,
                                List<String> tagsToAdd) {
        try {
            // 读取当前标签
            String currentTagsString = getCurrentAdditionalTags(inspection);
            List<String> currentTags = parseTagsString(currentTagsString);

            // 合并标签（去重）
            Set<String> allTags = new HashSet<>(currentTags);
            allTags.addAll(tagsToAdd);

            // 重新组合为字符串
            String newTagsString = allTags.stream().sorted().collect(Collectors.joining(","));

            // 设置新标签字符串
            setAdditionalTags(inspection, newTagsString);
        } catch (Exception e) {
            // 如果批量添加失败，尝试逐个添加
            for (String tag : tagsToAdd) {
                registerAdditionalTag(inspection, tag);
            }
        }
    }

    /**
     * 从 JavadocDeclarationInspection 中删除标签
     *
     * @param inspection   Javadoc 检查工具实例
     * @param tagsToRemove 要删除的标签列表
     */
    private static void removeTags(JavadocDeclarationInspection inspection,
                                   List<String> tagsToRemove) {
        try {
            // 读取当前标签
            String currentTagsString = getCurrentAdditionalTags(inspection);
            List<String> currentTags = parseTagsString(currentTagsString);

            // 过滤掉要删除的标签
            Set<String> tagsToRemoveSet = new HashSet<>(tagsToRemove);
            List<String> remainingTags = currentTags.stream()
                .filter(tag -> !tagsToRemoveSet.contains(tag))
                .toList();

            // 重新组合为字符串
            String newTagsString = remainingTags.isEmpty()
                                   ? ""
                                   : remainingTags.stream().sorted().collect(Collectors.joining(","));

            // 设置新标签字符串
            setAdditionalTags(inspection, newTagsString);
        } catch (Exception e) {
            // 删除失败，静默处理
        }
    }

    /**
     * 注册额外的标签到 Javadoc 检查工具中
     * <p>
     * 这是原有的方法，保留用于向后兼容和备用方案
     *
     * @param inspection Javadoc 检查工具实例
     * @param tagName    要注册的标签名称
     */
    private static void registerAdditionalTag(JavadocDeclarationInspection inspection,
                                              String tagName) {
        try {
            // 尝试直接访问 ADDITIONAL_TAGS 字段（推荐方式）
            java.lang.reflect.Field additionalTagsField =
                JavadocDeclarationInspection.class.getDeclaredField("ADDITIONAL_TAGS");
            additionalTagsField.setAccessible(true);
            String additionalTags = (String) additionalTagsField.get(inspection);

            // 如果标签不存在，则添加
            if (additionalTags == null || additionalTags.isEmpty()) {
                additionalTagsField.set(inspection, tagName);
            } else {
                // 检查标签是否已存在（不区分大小写）
                List<String> existingTags = parseTagsString(additionalTags);
                String tagNameLower = tagName.toLowerCase();
                if (!existingTags.contains(tagNameLower)) {
                    additionalTagsField.set(inspection, additionalTags + "," + tagName);
                }
            }
        } catch (Exception e) {
            // 如果直接访问字段失败，尝试使用反射调用 registerAdditionalTag 方法
            try {
                java.lang.reflect.Method method =
                    JavadocDeclarationInspection.class.getDeclaredMethod("registerAdditionalTag",
                                                                         String.class);
                method.setAccessible(true);
                method.invoke(inspection, tagName);
            } catch (Exception ignored) {
            }
        }
    }

}
