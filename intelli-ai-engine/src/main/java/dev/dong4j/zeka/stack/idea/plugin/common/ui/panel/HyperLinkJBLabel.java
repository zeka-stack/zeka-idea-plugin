package dev.dong4j.zeka.stack.idea.plugin.common.ui.panel;

import com.intellij.ui.components.JBLabel;

import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkListener;

import dev.dong4j.zeka.stack.idea.plugin.common.ui.listener.HyperLinkListenerImpl;

/**
 * 超链接标签类
 * <p> 继承自 JBLabel, 提供带有超链接功能的标签控件
 * <p> 重写了 createHyperlinkListener 方法, 创建并返回一个 HyperlinkListener 实现, 用于处理超链接事件
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class HyperLinkJBLabel extends JBLabel {
    /**
     * 创建并返回一个自定义的超链接监听器
     * <p> 该方法重写父类的方法, 返回一个具体的超链接监听器实现
     *
     * @return 自定义的超链接监听器实例, 不能为 null
     */
    @Override
    protected @NotNull HyperlinkListener createHyperlinkListener() {
        return new HyperLinkListenerImpl();
    }
}

