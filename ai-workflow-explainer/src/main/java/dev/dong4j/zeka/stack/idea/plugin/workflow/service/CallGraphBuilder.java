package dev.dong4j.zeka.stack.idea.plugin.workflow.service;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.dong4j.zeka.stack.idea.plugin.workflow.model.MethodInfo;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.MethodContextExtractor;

/**
 * 调用链构建器
 *
 * @author dong4j
 * @version 1.0.0
 */
public class CallGraphBuilder {
    private static final int MAX_DEPTH = 2; // 限制深度，避免性能问题
    private static final int MAX_CALLERS = 5; // 最多查找的调用者数量
    private static final int MAX_CALLEES = 10; // 最多查找的被调用者数量

    private final Project project;
    private final Set<PsiMethod> visited = new HashSet<>();

    public CallGraphBuilder(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 查找调用者（谁调用了该方法）
     *
     * @param method 目标方法
     * @return 调用者方法列表
     */
    @NotNull
    public List<MethodInfo> findCallers(@NotNull PsiMethod method) {
        List<MethodInfo> callers = new ArrayList<>();
        visited.clear();
        findCallersRecursive(method, callers, 0);
        return callers;
    }

    /**
     * 递归查找调用者
     *
     * @param method  目标方法
     * @param callers 调用者列表
     * @param depth   当前深度
     */
    private void findCallersRecursive(@NotNull PsiMethod method, @NotNull List<MethodInfo> callers, int depth) {
        if (depth > MAX_DEPTH || callers.size() >= MAX_CALLERS || visited.contains(method)) {
            return;
        }
        visited.add(method);

        try {
            ReferencesSearch.search(method, GlobalSearchScope.projectScope(project))
                .forEach(ref -> {
                    if (callers.size() >= MAX_CALLERS) {
                        return;
                    }
                    PsiElement element = ref.getElement();
                    PsiMethod callerMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
                    if (callerMethod != null && !callerMethod.equals(method)) {
                        // 避免重复添加
                        boolean alreadyAdded = callers.stream()
                            .anyMatch(c -> c.qualifiedClassName.equals(getQualifiedClassName(callerMethod))
                                           && c.name.equals(callerMethod.getName()));
                        if (!alreadyAdded) {
                            MethodInfo callerInfo = MethodContextExtractor.extractMethodInfo(callerMethod);
                            callers.add(callerInfo);
                        }
                    }
                });
        } catch (Exception e) {
            // 静默处理异常，避免影响功能
        }
    }

    /**
     * 查找被调用者（该方法调用了哪些方法）
     *
     * @param method 目标方法
     * @return 被调用者方法列表
     */
    @NotNull
    public List<MethodInfo> findCallees(@NotNull PsiMethod method) {
        List<MethodInfo> callees = new ArrayList<>();
        visited.clear();
        findCalleesRecursive(method, callees, 0);
        return callees;
    }

    /**
     * 递归查找被调用者
     *
     * @param method  目标方法
     * @param callees 被调用者列表
     * @param depth   当前深度
     */
    private void findCalleesRecursive(@NotNull PsiMethod method, @NotNull List<MethodInfo> callees, int depth) {
        if (depth > MAX_DEPTH || callees.size() >= MAX_CALLEES || visited.contains(method)) {
            return;
        }
        visited.add(method);

        com.intellij.psi.PsiCodeBlock body = method.getBody();
        if (body == null) {
            return;
        }

        body.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                if (callees.size() >= MAX_CALLEES) {
                    return;
                }
                PsiMethod calledMethod = expression.resolveMethod();
                if (calledMethod != null && !calledMethod.equals(method)) {
                    // 避免重复添加
                    boolean alreadyAdded = callees.stream()
                        .anyMatch(c -> c.qualifiedClassName.equals(getQualifiedClassName(calledMethod))
                                       && c.name.equals(calledMethod.getName()));
                    if (!alreadyAdded) {
                        MethodInfo calleeInfo = MethodContextExtractor.extractMethodInfo(calledMethod);
                        callees.add(calleeInfo);
                    }
                }
            }
        });
    }

    /**
     * 获取方法的完整限定类名
     *
     * @param method 方法
     * @return 完整限定类名
     */
    @NotNull
    private String getQualifiedClassName(@NotNull PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
            String qualifiedName = containingClass.getQualifiedName();
            return qualifiedName != null ? qualifiedName : "";
        }
        return "";
    }
}

