package dev.dong4j.zeka.stack.idea.plugin.template.live;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;

import org.jetbrains.annotations.Nullable;

/**
 * Live Template 提供者
 * <p>
 * 该类用于提供 Live Template 文件路径配置，主要负责定义默认的 Live Template 文件位置。
 * 适用于 IDE 中的代码补全和模板功能，支持统一管理模板文件的加载路径。
 * 这是 ZKS Dev Helper 插件中代码样式模块的组件。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.25
 * @since 1.0.0
 */
public class UniformLiveTemplateProvider implements DefaultLiveTemplatesProvider {

    /**
     * 获取默认的直播模板文件列表
     * <p>
     * 返回系统预设的直播模板文件路径数组，用于初始化直播模板配置。
     *
     * @return 默认直播模板文件路径数组
     */
    @Override
    public String[] getDefaultLiveTemplateFiles() {
        return new String[] {
            "liveTemplates/zeka-stack-live-template"
        };
    }

    /**
     * 获取隐藏的实时模板文件列表
     * <p>
     * 返回系统中被标记为隐藏的实时模板文件名称数组。
     *
     * @return 隐藏的实时模板文件名称数组，可能为 null
     */
    @Nullable
    @Override
    public String[] getHiddenLiveTemplateFiles() {
        return null;
    }
}
