package dev.dong4j.zeka.stack.idea.javadoc.semantics;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 类语义分析器
 * <p>
 * 负责通过 PSI API 分析类的各种语义信息，填充 {@link ClassSemanticModel}。
 * 包括架构位置、职责、暴露范围、使用场景、依赖关系和设计意图等维度的分析。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.8.0
 */
public class ClassSemanticAnalyzer {

    /**
     * 分析类的语义信息
     *
     * @param psiClass 要分析的类
     * @param project  项目对象
     * @return 类的语义模型
     */
    @NotNull
    public ClassSemanticModel analyze(@NotNull PsiClass psiClass, @NotNull Project project) {
        ClassSemanticModel model = new ClassSemanticModel();

        // 1. 分析架构位置
        model.setLayer(resolveLayer(psiClass));

        // 2. 分析职责
        model.setResponsibility(resolveResponsibility(psiClass));

        // 3. 分析暴露范围
        model.setExposure(resolveExposure(psiClass, project));

        // 4. 分析使用场景
        analyzeUsageContext(psiClass, project, model);

        // 5. 分析依赖关系
        analyzeDependencies(psiClass, model);

        // 6. 推断设计意图
        inferDesignIntent(psiClass, model);

        return model;
    }

    /**
     * 解析类在分层架构中的位置
     *
     * @param psiClass 要分析的类
     * @return 架构层级（控制器 / 服务层 / 仓库层 / 领域层 / 基础设施层 / 未知）
     */
    @NotNull
    private String resolveLayer(@NotNull PsiClass psiClass) {
        // 1. 优先检查注解
        if (AnnotationUtil.isAnnotated(psiClass,
                                       "org.springframework.web.bind.annotation.RestController", 0)) {
            return "控制器";
        }
        if (AnnotationUtil.isAnnotated(psiClass,
                                       "org.springframework.stereotype.Service", 0)) {
            return "服务层";
        }
        if (AnnotationUtil.isAnnotated(psiClass,
                                       "org.springframework.stereotype.Repository", 0)) {
            return "仓库层";
        }
        if (AnnotationUtil.isAnnotated(psiClass,
                                       "org.springframework.stereotype.Component", 0)) {
            return "组件";
        }

        // 2. 检查包名
        PsiFile containingFile = psiClass.getContainingFile();
        if (containingFile instanceof PsiJavaFile) {
            String packageName = ((PsiJavaFile) containingFile).getPackageName();
            if (packageName.contains(".controller") || packageName.contains(".web")) {
                return "控制器";
            }
            if (packageName.contains(".service") || packageName.contains(".biz")) {
                return "服务层";
            }
            if (packageName.contains(".repository") || packageName.contains(".dao")) {
                return "仓库层";
            }
            if (packageName.contains(".domain") || packageName.contains(".model")
                || packageName.contains(".entity")) {
                return "领域层";
            }
            if (packageName.contains(".infrastructure") || packageName.contains(".infra")) {
                return "基础设施层";
            }
        }

        // 3. 默认返回未知（后续可通过调用者类型推断）
        return "未知";
    }

    /**
     * 解析类的主要职责
     *
     * @param psiClass 要分析的类
     * @return 职责描述
     */
    @NotNull
    private String resolveResponsibility(@NotNull PsiClass psiClass) {
        // 1. 从类名提取领域关键词
        String className = psiClass.getName();
        if (className == null) {
            return "业务逻辑";
        }
        String domain = extractDomainFromClassName(className);

        // 2. 统计方法名中的动词
        Set<String> verbs = new LinkedHashSet<>();
        for (PsiMethod method : psiClass.getMethods()) {
            if (method.isConstructor() || method.hasModifierProperty(PsiModifier.PRIVATE)) {
                continue;
            }
            String methodName = method.getName();
            String verb = extractVerb(methodName);
            if (verb != null) {
                verbs.add(verb);
            }
        }

        // 3. 映射动词到职责描述
        String responsibility = mapVerbsToResponsibility(verbs);

        // 4. 组合领域和职责
        if (domain != null && !domain.isEmpty()) {
            return domain + " " + responsibility;
        }
        return responsibility;
    }

    /**
     * 从类名提取领域关键词
     */
    @Nullable
    private String extractDomainFromClassName(@NotNull String className) {
        // 移除常见后缀：Service, Controller, Repository, Manager, Handler 等
        String domain = className
            .replaceAll("Service$", "")
            .replaceAll("Controller$", "")
            .replaceAll("Repository$", "")
            .replaceAll("Manager$", "")
            .replaceAll("Handler$", "")
            .replaceAll("Impl$", "");

        // 转换为小写，便于理解
        return domain.isEmpty() ? null : domain.toLowerCase();
    }

    /**
     * 从方法名提取动词
     */
    @Nullable
    private String extractVerb(@NotNull String methodName) {
        // 常见动词模式
        if (methodName.startsWith("create") || methodName.startsWith("register")) {
            return "creation";
        }
        if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "modification";
        }
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "deletion";
        }
        if (methodName.startsWith("find") || methodName.startsWith("get")
            || methodName.startsWith("query") || methodName.startsWith("search")) {
            return "query";
        }
        if (methodName.startsWith("validate") || methodName.startsWith("check")) {
            return "validation";
        }
        if (methodName.startsWith("publish") || methodName.startsWith("send")
            || methodName.startsWith("emit")) {
            return "event_dispatching";
        }
        return null;
    }

    /**
     * 映射动词到职责描述
     */
    @NotNull
    private String mapVerbsToResponsibility(@NotNull Set<String> verbs) {
        if (verbs.contains("creation")) {
            return "创建和管理逻辑";
        }
        if (verbs.contains("query")) {
            return "查询和检索逻辑";
        }
        if (verbs.contains("validation")) {
            return "验证逻辑";
        }
        if (verbs.contains("event_dispatching")) {
            return "事件分发逻辑";
        }
        return "业务逻辑";
    }

    /**
     * 解析类的暴露范围
     *
     * @param psiClass 要分析的类
     * @param project  项目对象
     * @return 暴露范围（仅内部使用 / 作为公共API暴露）
     */
    @NotNull
    private String resolveExposure(@NotNull PsiClass psiClass, @NotNull Project project) {
        // 1. Controller 一律认为是 external
        if (isController(psiClass)) {
            return "作为公共API暴露";
        }

        // 2. 检查是否被其他模块引用
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        Collection<PsiReference> references = ReferencesSearch.search(psiClass, projectScope).findAll();

        for (PsiReference ref : references) {
            PsiElement element = ref.getElement();
            PsiClass caller = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (caller != null && !sameModule(caller, psiClass)) {
                return "作为公共API暴露";
            }
        }

        return "仅内部使用";
    }

    /**
     * 判断两个类是否在同一模块
     */
    private boolean sameModule(@NotNull PsiClass class1, @NotNull PsiClass class2) {
        PsiFile file1 = class1.getContainingFile();
        PsiFile file2 = class2.getContainingFile();
        if (file1 == null || file2 == null) {
            return false;
        }

        com.intellij.openapi.vfs.VirtualFile vf1 = file1.getVirtualFile();
        com.intellij.openapi.vfs.VirtualFile vf2 = file2.getVirtualFile();
        if (vf1 == null || vf2 == null) {
            return false;
        }

        // 简单判断：同一包路径或同一模块路径
        String path1 = vf1.getPath();
        String path2 = vf2.getPath();

        // 提取模块路径（假设模块在独立的目录下）
        // 这里可以根据实际项目结构调整
        return path1.contains("/src/main/java/") && path2.contains("/src/main/java/");
    }

    /**
     * 分析类的使用场景
     *
     * @param psiClass 要分析的类
     * @param project  项目对象
     * @param model    语义模型（用于填充结果）
     */
    private void analyzeUsageContext(@NotNull PsiClass psiClass,
                                     @NotNull Project project,
                                     @NotNull ClassSemanticModel model) {
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        Collection<PsiReference> references = ReferencesSearch.search(psiClass, projectScope).findAll();

        Set<String> callerTypes = new LinkedHashSet<>();

        for (PsiReference ref : references) {
            PsiElement element = ref.getElement();
            PsiClass caller = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (caller == null) {
                continue;
            }

            // 识别调用者类型（架构角色）
            if (isController(caller)) {
                callerTypes.add("REST控制器");
            } else if (isJob(caller)) {
                callerTypes.add("定时任务");
            } else if (isListener(caller)) {
                callerTypes.add("事件监听器");
            } else if (isService(caller)) {
                callerTypes.add("服务组件");
            } else if (isRepository(caller)) {
                callerTypes.add("数据访问层");
            } else if (isComponent(caller)) {
                callerTypes.add("组件");
            }
        }

        model.setCallerTypes(callerTypes);
    }

    /**
     * 判断是否是 Controller
     */
    private boolean isController(@NotNull PsiClass psiClass) {
        return AnnotationUtil.isAnnotated(psiClass,
                                          "org.springframework.web.bind.annotation.RestController", 0)
               || AnnotationUtil.isAnnotated(psiClass,
                                             "org.springframework.stereotype.Controller", 0);
    }

    /**
     * 判断是否是定时任务
     */
    private boolean isJob(@NotNull PsiClass psiClass) {
        return AnnotationUtil.isAnnotated(psiClass,
                                          "org.springframework.scheduling.annotation.Scheduled", 0)
               || (psiClass.getName() != null && psiClass.getName().contains("Job"));
    }

    /**
     * 判断是否是事件监听器
     */
    private boolean isListener(@NotNull PsiClass psiClass) {
        return AnnotationUtil.isAnnotated(psiClass,
                                          "org.springframework.context.event.EventListener", 0)
               || (psiClass.getName() != null && psiClass.getName().contains("Listener"));
    }

    /**
     * 判断是否是 Service
     */
    private boolean isService(@NotNull PsiClass psiClass) {
        return AnnotationUtil.isAnnotated(psiClass,
                                          "org.springframework.stereotype.Service", 0);
    }

    /**
     * 判断是否是 Repository
     */
    private boolean isRepository(@NotNull PsiClass psiClass) {
        return AnnotationUtil.isAnnotated(psiClass,
                                          "org.springframework.stereotype.Repository", 0);
    }

    /**
     * 判断是否是 Component
     */
    private boolean isComponent(@NotNull PsiClass psiClass) {
        return AnnotationUtil.isAnnotated(psiClass,
                                          "org.springframework.stereotype.Component", 0);
    }

    /**
     * 分析类的依赖关系
     *
     * @param psiClass 要分析的类
     * @param model    语义模型（用于填充结果）
     */
    private void analyzeDependencies(@NotNull PsiClass psiClass, @NotNull ClassSemanticModel model) {
        Set<String> dependencies = new LinkedHashSet<>();
        Set<String> sideEffects = new LinkedHashSet<>();

        // 1. 分析字段注入
        for (PsiField field : psiClass.getFields()) {
            if (AnnotationUtil.isAnnotated(field,
                                           "org.springframework.beans.factory.annotation.Autowired", 0)
                || hasConstructorInjection(field)) {

                PsiType type = field.getType();
                String typeName = type.getPresentableText();

                // 类型语义映射
                String semanticDescription = mapTypeToSemantic(typeName);
                if (semanticDescription != null) {
                    dependencies.add(semanticDescription);
                }
            }
        }

        // 2. 分析方法调用中的副作用
        for (PsiMethod method : psiClass.getMethods()) {
            if (method.isConstructor()) {
                continue;
            }

            // 检查方法体中是否有事件发布、IO 操作等
            PsiCodeBlock body = method.getBody();
            if (body != null) {
                if (containsMethodCall(body, "publish", "send", "emit")) {
                    sideEffects.add("发布领域事件");
                }
                if (containsMethodCall(body, "save", "persist", "flush")) {
                    sideEffects.add("执行数据库操作");
                }
            }
        }

        model.setDependencies(dependencies);
        model.setSideEffects(sideEffects);
    }

    /**
     * 检查字段是否有构造函数注入
     */
    private boolean hasConstructorInjection(@NotNull PsiField field) {
        PsiClass containingClass = PsiTreeUtil.getParentOfType(field, PsiClass.class);
        if (containingClass == null) {
            return false;
        }

        // 检查构造函数参数中是否包含该字段类型
        for (PsiMethod constructor : containingClass.getConstructors()) {
            for (PsiParameter parameter : constructor.getParameterList().getParameters()) {
                PsiType parameterType = parameter.getType();
                if (parameterType.equals(field.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 将类型映射到语义描述
     */
    @Nullable
    private String mapTypeToSemantic(@NotNull String typeName) {
        // Repository 模式
        if (typeName.endsWith("Repository") || typeName.endsWith("DAO")) {
            return typeName + " (数据库访问)";
        }

        // Client / Feign 模式
        if (typeName.endsWith("Client") || typeName.endsWith("Feign")) {
            return typeName + " (远程服务)";
        }

        // Publisher / Producer 模式
        if (typeName.endsWith("Publisher") || typeName.endsWith("Producer")) {
            return typeName + " (事件发布)";
        }

        // Template 模式（如 RestTemplate, JdbcTemplate）
        if (typeName.endsWith("Template")) {
            return typeName + " (模板操作)";
        }

        // Mapper 模式
        if (typeName.endsWith("Mapper")) {
            return typeName + " (数据映射)";
        }

        return null;
    }

    /**
     * 检查方法体中是否包含指定方法调用
     */
    private boolean containsMethodCall(@NotNull PsiCodeBlock body,
                                       @NotNull String... methodNames) {
        // 使用 PsiRecursiveElementVisitor 遍历方法体
        AtomicBoolean found = new AtomicBoolean(false);
        body.accept(new JavaRecursiveElementVisitor() {
            /**
             * 处理方法调用表达式, 用于检测是否调用了指定的方法名
             * <p> 遍历方法调用表达式, 检查被调用的方法名是否与预定义的方法名集合中的任意一个匹配, 若匹配则设置标志位为 true 并返回
             *
             * @param expression 被访问的方法调用表达式
             */
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                String calledMethodName = expression.getMethodExpression().getReferenceName();
                if (calledMethodName != null) {
                    for (String methodName : methodNames) {
                        if (calledMethodName.toLowerCase().contains(methodName.toLowerCase())) {
                            found.set(true);
                            return;
                        }
                    }
                }
                super.visitMethodCallExpression(expression);
            }
        });
        return found.get();
    }

    /**
     * 推断类的设计意图
     *
     * @param psiClass 要分析的类
     * @param model    语义模型（用于填充结果）
     */
    private void inferDesignIntent(@NotNull PsiClass psiClass, @NotNull ClassSemanticModel model) {
        Set<String> intents = new LinkedHashSet<>();

        // 1. 检查是否有基础设施依赖
        boolean hasInfrastructureDependency = false;
        for (PsiField field : psiClass.getFields()) {
            PsiType type = field.getType();
            String typeName = type.getPresentableText();
            if (typeName.contains("Repository") || typeName.contains("Client")
                || typeName.contains("Template")) {
                hasInfrastructureDependency = true;
                break;
            }
        }
        if (!hasInfrastructureDependency) {
            intents.add("避免基础设施关注");
        }

        // 2. 检查方法名是否偏业务
        boolean hasBusinessMethods = false;
        for (PsiMethod method : psiClass.getMethods()) {
            if (method.isConstructor()) {
                continue;
            }
            String methodName = method.getName().toLowerCase();
            if (methodName.contains("create") || methodName.contains("validate")
                || methodName.contains("calculate") || methodName.contains("process")) {
                hasBusinessMethods = true;
                break;
            }
        }
        if (hasBusinessMethods) {
            intents.add("封装业务规则");
        }

        // 3. 检查是否是工具类
        boolean isUtilClass = psiClass.hasModifierProperty(PsiModifier.FINAL)
                              && hasOnlyStaticMethods(psiClass);
        if (!isUtilClass) {
            intents.add("面向对象设计");
        }

        // 4. 检查是否是 Controller
        if (!isController(psiClass)) {
            intents.add("不负责请求处理");
        }

        model.setDesignIntents(intents);
    }

    /**
     * 检查类是否只有静态方法
     */
    private boolean hasOnlyStaticMethods(@NotNull PsiClass psiClass) {
        for (PsiMethod method : psiClass.getMethods()) {
            if (!method.isConstructor() && !method.hasModifierProperty(PsiModifier.STATIC)) {
                return false;
            }
        }
        return true;
    }
}
