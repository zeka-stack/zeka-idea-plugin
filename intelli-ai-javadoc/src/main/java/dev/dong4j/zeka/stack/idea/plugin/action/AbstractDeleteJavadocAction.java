package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;

import dev.dong4j.zeka.stack.idea.plugin.service.JavadocDeletionService;

/**
 * 抽象删除 Javadoc 动作类
 * <p>
 * 提供删除 Javadoc 的基础功能，子类可以继承此类实现具体的删除逻辑。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.6.0
 */
public abstract class AbstractDeleteJavadocAction extends AnAction {

    /** 删除服务实例 */
    protected final JavadocDeletionService deletionService = new JavadocDeletionService();
}

