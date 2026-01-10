package dev.dong4j.zeka.stack.idea.javadoc.semantics;

import org.jetbrains.annotations.NotNull;

/**
 * 语义提示构建器类
 * <p> 用于根据提供的语义模型构建类注释的提示文本, 主要应用于生成文档注释或进行代码分析时提供上下文信息
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2026.01.10
 * @since 1.0.0
 */
public class SemanticPromptBuilder {

    /**
     * 构建类注释的语义上下文 Prompt
     *
     * @param model 类的语义模型
     * @return Prompt 文本
     */
    @NotNull
    public String buildClassSemanticPrompt(@NotNull ClassSemanticModel model) {
        StringBuilder prompt = new StringBuilder();
        boolean hasAnyInfo = false;

        // 类的角色摘要
        boolean hasRoleInfo = false;
        if (model.getLayer() != null && !model.getLayer().isEmpty()) {
            prompt.append("- 架构层级：").append(model.getLayer()).append("\n");
            hasRoleInfo = true;
            hasAnyInfo = true;
        }
        if (model.getResponsibility() != null && !model.getResponsibility().isEmpty()) {
            prompt.append("- 主要职责：").append(model.getResponsibility()).append("\n");
            hasRoleInfo = true;
            hasAnyInfo = true;
        }
        if (model.getExposure() != null && !model.getExposure().isEmpty()) {
            prompt.append("- 暴露范围：").append(model.getExposure()).append("\n");
            hasRoleInfo = true;
            hasAnyInfo = true;
        }

        if (hasRoleInfo) {
            prompt.insert(0, "### 语义上下文\n类的角色摘要：\n");
        }

        // 使用场景
        if (!model.getCallerTypes().isEmpty()) {
            if (hasAnyInfo) {
                prompt.append("\n");
            } else {
                prompt.append("### 语义上下文\n");
            }
            prompt.append("使用场景：\n");
            prompt.append("- 主要由 ").append(String.join("、", model.getCallerTypes())).append(" 调用\n");
            hasAnyInfo = true;
        }

        // 依赖关系
        if (!model.getDependencies().isEmpty() || !model.getSideEffects().isEmpty()) {
            if (hasAnyInfo) {
                prompt.append("\n");
            } else {
                prompt.append("### 语义上下文\n");
            }
            prompt.append("依赖关系：\n");
            for (String dependency : model.getDependencies()) {
                prompt.append("- ").append(dependency).append("\n");
            }
            for (String sideEffect : model.getSideEffects()) {
                prompt.append("- ").append(sideEffect).append("\n");
            }
            hasAnyInfo = true;
        }

        // 设计意图
        if (!model.getDesignIntents().isEmpty()) {
            if (hasAnyInfo) {
                prompt.append("\n");
            } else {
                prompt.append("### 语义上下文\n");
            }
            prompt.append("设计意图：\n");
            for (String intent : model.getDesignIntents()) {
                prompt.append("- ").append(intent).append("\n");
            }
            hasAnyInfo = true;
        }

        // 如果没有任何信息，返回空字符串（不显示语义上下文部分）
        if (!hasAnyInfo) {
            return "";
        }

        return prompt.toString();
    }
}
