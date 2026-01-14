package dev.dong4j.zeka.stack.idea.plugin.workflow.service;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.dong4j.zeka.stack.idea.plugin.workflow.model.ClassInfo;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.ClassRelationshipContext;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.MethodCallerChainContext;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.MethodInfo;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.MethodContextExtractor;

/**
 * 调用链构建器
 *
 * @author dong4j
 * @version 1.0.0
 */
public class CallGraphBuilder {
    /** 限制调用链递归深度, 避免性能问题 */
    private static final int MAX_DEPTH = 2; // 限制深度，避免性能问题
    /** 最多查找的调用者数量 */
    private static final int MAX_CALLERS = 5; // 最多查找的调用者数量
    /** 最多查找的被调用者数量 */
    private static final int MAX_CALLEES = 10; // 最多查找的被调用者数量

    /** 项目上下文, 用于获取当前项目范围的搜索作用域和资源 */
    private final Project project;
    /** 用于记录已访问的方法, 避免递归查找时重复处理 */
    private final Set<PsiMethod> visited = new HashSet<>();

    /**
     * 构造函数, 初始化调用链构建器
     * <p> 用于创建一个调用链构建器实例, 传入项目上下文以支持后续的调用关系分析
     *
     * @param project 项目上下文, 用于在后续分析中访问项目范围的元素
     */
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
                        return false;
                    }
                    PsiElement element = ref.getElement();
                    PsiMethod callerMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
                    addCallers(method, callers, callerMethod);
                    return true;
                });
        } catch (Exception e) {
            // 静默处理异常，避免影响功能
        }
    }

    /**
     * 处理方法调用者信息, 避免重复添加
     * <p> 检查传入的调用者方法是否已存在于调用者列表中, 若未存在则提取其上下文信息并添加到列表中
     *
     * @param method       目标方法, 用于获取调用者上下文
     * @param callers      调用者方法列表, 用于去重和添加新调用者
     * @param callerMethod 调用者方法, 可能为 null, 若不为 null 且不等于目标方法, 则视为有效调用者
     */
    private void addCallers(@NotNull PsiMethod method, @NotNull List<MethodInfo> callers, PsiMethod callerMethod) {
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
            /**
             * 重写方法调用表达式访问器, 处理方法调用节点
             * <p> 在访问方法调用表达式时, 若调用链长度已达到最大限制, 则直接返回; 否则解析被调用方法并执行相关处理逻辑
             *
             * @param expression 方法调用表达式节点, 非空
             */
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                if (callees.size() >= MAX_CALLEES) {
                    return;
                }
                PsiMethod calledMethod = expression.resolveMethod();
                addCallers(method, callees, calledMethod);
            }
        });
    }

    /**
     * 查找方法的调用链（谁调用了这个方法）
     *
     * @param method 目标方法
     * @return 方法调用链上下文
     */
    @NotNull
    public MethodCallerChainContext findMethodCallerChain(@NotNull PsiMethod method) {
        MethodCallerChainContext context = new MethodCallerChainContext();

        // 设置目标方法信息
        context.targetMethod = MethodContextExtractor.extractMethodInfo(method);

        // 查找直接调用者
        context.directCallers = findCallers(method);

        // 查找被调用者
        context.callees = findCallees(method);

        // 构建调用链（简化版，只构建一层）
        buildCallerChains(method, context);

        return context;
    }

    /**
     * 构建调用链
     *
     * @param method  目标方法
     * @param context 上下文
     */
    private void buildCallerChains(@NotNull PsiMethod method, @NotNull MethodCallerChainContext context) {
        for (MethodInfo caller : context.directCallers) {
            MethodCallerChainContext.CallerChain chain = new MethodCallerChainContext.CallerChain();
            chain.chain.add(caller);
            chain.chain.add(context.targetMethod);
            chain.depth = 1;
            context.callerChains.add(chain);
        }
    }

    /**
     * 查找类的继承关系
     *
     * @param psiClass 目标类
     * @return 继承关系信息
     */
    @NotNull
    public ClassRelationshipContext.InheritanceInfo findClassInheritance(@NotNull PsiClass psiClass) {
        ClassRelationshipContext.InheritanceInfo inheritance = new ClassRelationshipContext.InheritanceInfo();

        // 查找父类
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null && !"java.lang.Object".equals(superClass.getQualifiedName())) {
            inheritance.superClass = MethodContextExtractor.extractClassInfo(superClass);
        }

        // 查找实现的接口
        PsiClassType[] implementsList = psiClass.getImplementsList() != null ?
                                        psiClass.getImplementsList().getReferencedTypes() : new PsiClassType[0];
        for (PsiClassType interfaceType : implementsList) {
            PsiClass interfaceClass = interfaceType.resolve();
            if (interfaceClass != null) {
                inheritance.interfaces.add(MethodContextExtractor.extractClassInfo(interfaceClass));
            }
        }

        // 查找子类（限制数量，避免性能问题）
        findSubClasses(psiClass, inheritance.subClasses);

        // 如果是接口，查找实现类
        if (psiClass.isInterface()) {
            findImplementations(psiClass, inheritance.implementations);
        }

        return inheritance;
    }

    /**
     * 查找子类
     *
     * @param psiClass   目标类
     * @param subClasses 子类列表
     */
    private void findSubClasses(@NotNull PsiClass psiClass, @NotNull List<ClassInfo> subClasses) {
        try {
            ClassInheritorsSearch.search(psiClass, GlobalSearchScope.projectScope(project), true)
                .forEach(subClass -> {
                    if (subClasses.size() >= MAX_CALLERS) {
                        return false;
                    }
                    subClasses.add(MethodContextExtractor.extractClassInfo(subClass));
                    return true;
                });
        } catch (Exception e) {
            // 静默处理异常
        }
    }

    /**
     * 查找接口实现类
     *
     * @param psiInterface    接口类
     * @param implementations 实现类列表
     */
    private void findImplementations(@NotNull PsiClass psiInterface, @NotNull List<ClassInfo> implementations) {
        try {
            ClassInheritorsSearch.search(psiInterface, GlobalSearchScope.projectScope(project), true)
                .forEach(implClass -> {
                    if (implementations.size() >= MAX_CALLERS) {
                        return false;
                    }
                    if (!implClass.isInterface()) {
                        implementations.add(MethodContextExtractor.extractClassInfo(implClass));
                    }
                    return true;
                });
        } catch (Exception e) {
            // 静默处理异常
        }
    }

    /**
     * 查找类的依赖关系
     *
     * @param psiClass 目标类
     * @return 依赖关系列表
     */
    @NotNull
    public List<ClassRelationshipContext.ClassDependency> findClassDependencies(@NotNull PsiClass psiClass) {
        List<ClassRelationshipContext.ClassDependency> dependencies = new ArrayList<>();
        Set<String> processedClasses = new HashSet<>();

        // 分析字段依赖
        analyzeFieldDependencies(psiClass, dependencies, processedClasses);

        // 分析方法依赖
        analyzeMethodDependencies(psiClass, dependencies, processedClasses);

        // 分析注解依赖
        analyzeAnnotationDependencies(psiClass, dependencies, processedClasses);

        return dependencies;
    }

    /**
     * 分析字段依赖
     */
    private void analyzeFieldDependencies(@NotNull PsiClass psiClass,
                                          @NotNull List<ClassRelationshipContext.ClassDependency> dependencies,
                                          @NotNull Set<String> processedClasses) {
        PsiField[] fields = psiClass.getFields();
        for (PsiField field : fields) {
            PsiType fieldType = field.getType();
            if (fieldType instanceof PsiClassType classType) {
                PsiClass fieldClass = classType.resolve();
                if (fieldClass != null && isValidDependency(fieldClass, processedClasses)) {
                    ClassRelationshipContext.ClassDependency dependency =
                        new ClassRelationshipContext.ClassDependency(
                            MethodContextExtractor.extractClassInfo(fieldClass),
                            ClassRelationshipContext.DependencyType.FIELD
                        );
                    dependency.locations.add("字段: " + field.getName());
                    dependencies.add(dependency);
                    processedClasses.add(fieldClass.getQualifiedName());
                }
            }
        }
    }

    /**
     * 分析方法依赖
     */
    @SuppressWarnings("D")
    private void analyzeMethodDependencies(@NotNull PsiClass psiClass,
                                           @NotNull List<ClassRelationshipContext.ClassDependency> dependencies,
                                           @NotNull Set<String> processedClasses) {
        PsiMethod[] methods = psiClass.getMethods();
        for (PsiMethod method : methods) {
            // 分析参数依赖
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (PsiParameter parameter : parameters) {
                PsiType paramType = parameter.getType();
                if (paramType instanceof PsiClassType classType) {
                    PsiClass paramClass = classType.resolve();
                    if (paramClass != null && isValidDependency(paramClass, processedClasses)) {
                        ClassRelationshipContext.ClassDependency dependency =
                            new ClassRelationshipContext.ClassDependency(
                                MethodContextExtractor.extractClassInfo(paramClass),
                                ClassRelationshipContext.DependencyType.METHOD_PARAMETER
                            );
                        dependency.locations.add("方法参数: " + method.getName() + "(" + parameter.getName() + ")");
                        dependencies.add(dependency);
                        processedClasses.add(paramClass.getQualifiedName());
                    }
                }
            }

            // 分析返回值依赖
            PsiType returnType = method.getReturnType();
            if (returnType instanceof PsiClassType classType) {
                PsiClass returnClass = classType.resolve();
                if (returnClass != null && isValidDependency(returnClass, processedClasses)) {
                    ClassRelationshipContext.ClassDependency dependency =
                        new ClassRelationshipContext.ClassDependency(
                            MethodContextExtractor.extractClassInfo(returnClass),
                            ClassRelationshipContext.DependencyType.METHOD_RETURN
                        );
                    dependency.locations.add("方法返回值: " + method.getName());
                    dependencies.add(dependency);
                    processedClasses.add(returnClass.getQualifiedName());
                }
            }
        }
    }

    /**
     * 分析注解依赖
     */
    private void analyzeAnnotationDependencies(@NotNull PsiClass psiClass,
                                               @NotNull List<ClassRelationshipContext.ClassDependency> dependencies,
                                               @NotNull Set<String> processedClasses) {
        // 简化实现，暂时跳过注解依赖分析
    }

    /**
     * 检查是否是有效的依赖
     */
    private boolean isValidDependency(@NotNull PsiClass psiClass, @NotNull Set<String> processedClasses) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return false;
        }

        // 跳过已处理的类
        if (processedClasses.contains(qualifiedName)) {
            return false;
        }

        // 跳过 Java 标准库类
        return !qualifiedName.startsWith("java.") && !qualifiedName.startsWith("javax.");
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
