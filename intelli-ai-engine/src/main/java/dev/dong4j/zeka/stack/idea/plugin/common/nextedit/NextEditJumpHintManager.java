package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBViewport;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;

/**
 * 下一个编辑跳转提示管理器
 * <p> 用于在编辑器中管理“跳转到下一次编辑位置”的提示弹窗, 当目标行不可见时显示提示, 用户点击后自动滚动到目标位置.
 * <p> 该类实现 {@code Disposable} 接口, 负责在生命周期结束时清理弹窗和滚动监听器, 避免内存泄漏.
 * <p> 主要功能包括:
 * <ul>
 *   <li> 在编辑器滚动时动态判断目标行是否可见 </li>
 *   <li> 若目标行不可见, 则显示跳转提示弹窗 </li>
 *   <li> 弹窗支持点击关闭, 点击跳转, 点击外部关闭等交互 </li>
 *   <li> 弹窗位置根据目标行是否在可视区域下方动态调整 </li>
 * </ul>
 * <p> 使用示例:
 * <pre>{@code
 * NextEditJumpHintManager manager = new NextEditJumpHintManager(editor, offset, parentDisposable);
 * manager.showIfNeeded();
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class NextEditJumpHintManager implements Disposable {
    /** 编辑器实例, 用于获取滚动模型和绘制跳转提示信息 */
    private final Editor editor;
    /** 行起始偏移量, 用于定位编辑器中特定行的开始位置 */
    private final int lineStartOffset;
    /** 是否在创建时可见 */
    private final boolean wasVisibleOnCreation;
    /** 跳转提示弹窗 */
    private JBPopup jumpPopup;
    /**
     * 滚动区域监听器
     * <p> 用于监听编辑器的滚动区域变化, 并在需要时更新跳转提示的可见性
     *
     * @see #updateVisibility()
     */
    private VisibleAreaListener scrollListener;

    /**
     * 构造函数, 初始化 NextEditJumpHintManager 对象
     * <p> 该构造函数接收一个编辑器对象, 行起始偏移量和父级可处置对象作为参数, 用于管理跳转提示的显示逻辑
     *
     * @param editor           编辑器对象, 不能为空
     * @param lineStartOffset  行起始偏移量
     * @param parentDisposable 父级可处置对象, 不能为空
     */
    NextEditJumpHintManager(@NotNull Editor editor, int lineStartOffset, @NotNull Disposable parentDisposable) {
        this.editor = editor;
        this.lineStartOffset = lineStartOffset;
        this.wasVisibleOnCreation = isLineVisible(editor, lineStartOffset);
        Disposer.register(parentDisposable, this);
    }

    /**
     * 根据当前滚动状态决定是否显示跳转提示弹窗
     * <p> 如果未注册可见区域监听器, 则注册并绑定事件处理; 然后更新可见性状态
     * <p> 该方法通常在编辑器滚动或内容变化后被调用, 用于动态控制提示弹窗的显示与隐藏
     *
     */
    void showIfNeeded() {
        if (scrollListener == null) {
            scrollListener = this::onVisibleAreaChanged;
            ScrollingModel scrollingModel = editor.getScrollingModel();
            scrollingModel.addVisibleAreaListener(scrollListener);
        }
        updateVisibility();
    }

    /**
     * 释放资源并清理相关组件
     * <p> 当对象被销毁时调用, 用于释放跳转提示弹窗和滚动区域监听器, 确保内存不被泄漏
     *
     */
    @Override
    public void dispose() {
        if (jumpPopup != null) {
            Disposer.dispose(jumpPopup);
        }
        jumpPopup = null;
        if (scrollListener != null) {
            editor.getScrollingModel().removeVisibleAreaListener(scrollListener);
        }
        scrollListener = null;
    }

    /**
     * 当可见区域发生变化时调用, 用于更新跳转提示的可见性
     * <p> 此方法在可见区域监听器触发时被调用, 会调用 {@link #updateVisibility()} 方法来检查并更新跳转提示是否需要显示
     *
     * @param event 可见区域事件, 包含可见区域变化的信息
     */
    private void onVisibleAreaChanged(@NotNull VisibleAreaEvent event) {
        updateVisibility();
    }

    /**
     * 更新跳转提示框的可见性状态
     * <p> 根据当前行是否在可视区域内决定是否显示或隐藏跳转提示框.
     * 如果当前行可见, 则隐藏跳转提示框; 如果当前行不可见且跳转提示框未显示, 则显示跳转提示框.
     *
     */
    private void updateVisibility() {
        boolean isVisible = wasVisibleOnCreation || isLineVisible(editor, lineStartOffset);
        if (isVisible) {
            if (jumpPopup != null) {
                jumpPopup.dispose();
            }
            jumpPopup = null;
        } else if (jumpPopup == null) {
            showJumpPopup();
        }
    }

    /**
     * 显示跳转提示弹窗
     * <p> 在编辑器中创建并显示一个用于导航的跳转提示窗口, 根据目标行是否在可视区域下方决定其位置
     *
     */
    private void showJumpPopup() {
        if (jumpPopup != null) {
            jumpPopup.dispose();
        }
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        int targetLineY = editor.offsetToPoint2D(lineStartOffset).getY() > 0
                          ? editor.visualLineToY(editor.getDocument().getLineNumber(lineStartOffset))
                          : 0;
        boolean isTargetBelow = targetLineY > visibleArea.y + visibleArea.height;
        NextEditJumpHintRenderer renderer = new NextEditJumpHintRenderer(editor, isTargetBelow);
        JComponent component = renderer.createJumpHintComponent();
        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(component, null)
            .setResizable(false)
            .setMovable(true)
            .setRequestFocus(false)
            .setCancelOnClickOutside(true)
            .setShowBorder(false)
            .createPopup();
        popup.addListener(new JBPopupListener() {
            /**
             * 窗口关闭时的监听回调方法
             * <p> 当轻量级窗口关闭时被调用, 当前实现为空操作 (no-op)
             *
             * @param event 窗口关闭事件, 不能为 null
             */
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                // no-op
            }
        });
        jumpPopup = popup;
        JComponent editorComponent = editor.getContentComponent();
        Component parent = editorComponent.getParent();
        if (!(parent instanceof JBViewport viewport)) {
            return;
        }
        Point point = new Point(viewport.getWidth() / 2 - component.getPreferredSize().width / 2,
                                isTargetBelow ? viewport.getHeight() - 20 - component.getPreferredSize().height : 20);
        jumpPopup.show(new RelativePoint(viewport, point));
    }

    /**
     * 判断指定行是否在编辑器的可视区域内
     * <p> 根据给定的行起始偏移量计算该行的起始和结束位置, 并判断这些位置是否在当前可视区域范围内
     *
     * @param editor          编辑器实例
     * @param lineStartOffset 行的起始偏移量
     * @return 如果行在可视区域内则返回 true, 否则返回 false
     */
    private boolean isLineVisible(Editor editor, int lineStartOffset) {
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        double lineStartY = editor.offsetToPoint2D(lineStartOffset).getY();
        int lineHeight = editor.getLineHeight();
        double lineEndY = lineStartY + lineHeight;
        return lineStartY <= visibleArea.y + visibleArea.height && lineEndY >= visibleArea.y;
    }
}
