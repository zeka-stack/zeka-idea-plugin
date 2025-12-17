package dev.dong4j.zeka.stack.idea.plugin.util;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * Psi 元素定位器
 * <p>
 * 提供在 IDE 编辑器中定位 Java 代码元素 (方法, 字段, 类, 文件) 的功能,
 * 通过编辑器光标位置来确定当前选中的 Psi 元素类型和位置信息.
 * 主要用于代码分析, 文档生成等场景中确定当前操作的代码元素.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@SuppressWarnings( {"LoggingSimilarMessage", "DuplicatedCode"})
@Slf4j
public class PsiElementLocator {
    /**
     * 定位结果记录类
     * <p>
     * 用于封装代码元素定位操作的结果, 包含定位到的 Psi 元素, 定位类型以及是否为整个文件的标识
     * 提供了便捷的方法来判断定位结果的类型 (方法, 字段, 类等)
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
     */
    public record LocateResult(PsiElement element, LocateType type, boolean isWholeFile) {
        /**
         * 构造函数，用于初始化 LocateResult 对象
         * <p>
         * 根据传入的 PsiElement、定位类型和是否整个文件的标志来初始化对象
         *
         * @param element     要定位的 PsiElement 元素
         * @param type        定位类型，用于指定定位方式
         * @param isWholeFile 是否定位整个文件
         */
        public LocateResult(@NotNull PsiElement element, @NotNull LocateType type, boolean isWholeFile) {
            this.element = element;
            this.type = type;
            this.isWholeFile = isWholeFile;
        }

        /**
         * 获取当前元素
         * <p>
         * 返回当前表示的 PsiElement 对象。
         *
         * @return 当前表示的 PsiElement 对象
         */
        @Override
        @NotNull
        public PsiElement element() {
            return element;
        }

        /**
         * 获取定位类型
         * <p>
         * 返回当前定位的类型信息
         *
         * @return 定位类型
         */
        @Override
        @NotNull
        public LocateType type() {
            return type;
        }

        /**
         * 判断当前类型是否为方法类型
         * <p>
         * 检查当前对象的类型是否为 LocateType.METHOD
         *
         * @return 如果类型为方法类型，返回 true；否则返回 false
         */
        public boolean isMethod() {
            return type == LocateType.METHOD;
        }

        /**
         * 判断当前类型是否为字段类型
         * <p>
         * 检查当前对象的 type 字段是否等于 LocateType.FIELD，返回对应的布尔值
         *
         * @return 如果类型为字段类型，返回 true；否则返回 false
         */
        public boolean isField() {
            return type == LocateType.FIELD;
        }

        /**
         * 判断当前类型是否为类类型
         * <p>
         * 检查当前对象的类型是否为LocateType.CLASS
         *
         * @return 如果类型为类类型，返回true；否则返回false
         */
        public boolean isClass() {
            return type == LocateType.CLASS;
        }

        /**
         * 返回对象的字符串表示形式
         * <p>
         * 该方法重写 Object 类的 toString 方法，返回包含类型、是否为整个文件以及元素类名的信息字符串
         *
         * @return 对象的字符串表示
         */
        @NotNull
        @Override
        public String toString() {
            return String.format("LocateResult{type=%s, isWholeFile=%s, element=%s}",
                                 type, isWholeFile, element.getClass().getSimpleName());
        }
    }

    /**
     * 定位类型枚举
     * <p>
     * 定义了代码元素的定位类型, 用于标识方法, 字段, 类或文件等不同级别的代码元素
     * METHOD 表示方法级别的定位
     * FIELD 表示字段级别的定位
     * CLASS 表示类级别的定位
     * FILE 表示文件级别的定位
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.30
     * @since 1.0.0
     */
    public enum LocateType {
        /** 方法 */
        METHOD,
        /** 字段（成员变量） */
        FIELD,
        /** 类 */
        CLASS,
        /** 文件 */
        FILE
    }

    /**
     * 根据编辑器光标位置定位 PSI 元素
     *
     * <p>获取编辑器光标位置，调用偏移量定位方法。
     * 作为对外提供的主要接口方法。
     *
     * <p>处理流程：
     * <ol>
     *   <li>获取光标偏移量</li>
     *   <li>调用 locateElementAtOffset 方法</li>
     *   <li>返回定位结果</li>
     * </ol>
     *
     * @param editor  编辑器对象
     * @param psiFile PSI 文件对象
     * @return 定位结果，如果无法定位则返回 null
     * @see #locateElementAtOffset(PsiFile, int)
     * @see Editor#getCaretModel()
     */
    @Nullable
    public static LocateResult locateElement(@NotNull Editor editor, @NotNull PsiFile psiFile) {
        int offset = editor.getCaretModel().getOffset();
        return locateElementAtOffset(psiFile, offset);
    }

    /**
     * 根据偏移量定位 PSI 元素
     *
     * <p>核心定位算法，根据指定偏移量在 PSI 树中查找最合适的元素。
     * 按照预定义的优先级顺序进行查找。
     *
     * <p>定位算法：
     * <ol>
     *   <li>验证文件类型（必须是 Java 文件）</li>
     *   <li>获取偏移量处的 PSI 元素</li>
     *   <li>按优先级查找父元素：方法 → 字段 → 类</li>
     *   <li>特殊处理类声明行</li>
     *   <li>默认返回文件级别结果</li>
     * </ol>
     *
     * <p>特殊情况处理：
     * <ul>
     *   <li>非 Java 文件：返回 null</li>
     *   <li>无元素：返回文件级别结果</li>
     *   <li>类内部：设置 isWholeFile=true</li>
     *   <li>类声明：设置 isWholeFile=false</li>
     * </ul>
     *
     * @param psiFile PSI 文件对象
     * @param offset  光标偏移量
     * @return 定位结果，如果无法定位则返回 null
     * @see PsiTreeUtil#getParentOfType(PsiElement, Class)
     */
    @Nullable
    public static LocateResult locateElementAtOffset(@NotNull PsiFile psiFile, int offset) {
        // 处理 Java 文件
        if (psiFile instanceof PsiJavaFile) {
            return locateJavaElement(psiFile, offset);
        }
        // 处理 Kotlin 文件
        else if (psiFile instanceof KtFile) {
            return locateKotlinElement((KtFile) psiFile, offset);
        }

        log.debug("Not a supported file type: {}", psiFile.getName());
        return null;
    }

    /**
     * 定位 Java 元素
     */
    @NotNull
    private static LocateResult locateJavaElement(@NotNull PsiFile psiFile, int offset) {
        // 获取光标位置的元素
        PsiElement elementAtCaret = psiFile.findElementAt(offset);
        if (elementAtCaret == null) {
            log.debug("No element at offset: {}", offset);
            return new LocateResult(psiFile, LocateType.FILE, true);
        }

        log.debug("Element at caret: {} ({})", elementAtCaret.getText(), elementAtCaret.getClass().getSimpleName());

        // 1. 优先查找方法
        PsiMethod method = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod.class);
        if (method != null) {
            log.info("Located method: {}", method.getName());
            return new LocateResult(method, LocateType.METHOD, false);
        }

        // 2. 查找字段（成员变量）
        PsiField field = PsiTreeUtil.getParentOfType(elementAtCaret, PsiField.class);
        if (field != null) {
            log.info("Located field: {}", field.getName());
            return new LocateResult(field, LocateType.FIELD, false);
        }

        // 3. 查找类
        PsiClass psiClass = PsiTreeUtil.getParentOfType(elementAtCaret, PsiClass.class);
        if (psiClass != null) {
            // 检查是否在类声明行（类名附近）
            if (isOnClassDeclaration(elementAtCaret, psiClass)) {
                log.info("Located class (on declaration): {}", psiClass.getName());
                return new LocateResult(psiClass, LocateType.CLASS, false);
            } else {
                // 光标在类内部但不在特定成员上，为整个类生成
                log.info("Located class (inside class body): {}", psiClass.getName());
                return new LocateResult(psiClass, LocateType.CLASS, true);
            }
        }

        // 4. 默认为整个文件生成
        log.info("No specific element found, using whole file");
        return new LocateResult(psiFile, LocateType.FILE, true);
    }

    /**
     * 定位 Kotlin 元素
     */
    @NotNull
    private static LocateResult locateKotlinElement(@NotNull KtFile ktFile, int offset) {
        // 获取光标位置的元素
        PsiElement elementAtCaret = ktFile.findElementAt(offset);
        if (elementAtCaret == null) {
            log.debug("No element at offset: {}", offset);
            return new LocateResult(ktFile, LocateType.FILE, true);
        }

        log.debug("Element at caret: {} ({})", elementAtCaret.getText(), elementAtCaret.getClass().getSimpleName());

        // 1. 优先查找函数
        KtNamedFunction function = PsiTreeUtil.getParentOfType(elementAtCaret, KtNamedFunction.class);
        if (function != null) {
            log.info("Located function: {}", function.getName());
            return new LocateResult(function, LocateType.METHOD, false);
        }

        // 2. 查找属性
        KtProperty property = PsiTreeUtil.getParentOfType(elementAtCaret, KtProperty.class);
        if (property != null) {
            log.info("Located property: {}", property.getName());
            return new LocateResult(property, LocateType.FIELD, false);
        }

        // 3. 查找类/对象
        KtClassOrObject ktClass = PsiTreeUtil.getParentOfType(elementAtCaret, KtClassOrObject.class);
        if (ktClass != null) {
            // 检查是否在类声明行（类名附近）
            if (isOnKotlinClassDeclaration(elementAtCaret, ktClass)) {
                log.info("Located class (on declaration): {}", ktClass.getName());
                return new LocateResult(ktClass, LocateType.CLASS, false);
            } else {
                // 光标在类内部但不在特定成员上，为整个类生成
                log.info("Located class (inside class body): {}", ktClass.getName());
                return new LocateResult(ktClass, LocateType.CLASS, true);
            }
        }

        // 4. 默认为整个文件生成
        log.info("No specific element found, using whole file");
        return new LocateResult(ktFile, LocateType.FILE, true);
    }

    /**
     * 判断光标是否在 Kotlin 类声明行上
     */
    private static boolean isOnKotlinClassDeclaration(@NotNull PsiElement element, @NotNull KtClassOrObject ktClass) {
        // 获取类的名称标识符
        PsiElement nameReference = ktClass.getNameIdentifier();
        if (nameReference == null) {
            return false;
        }

        // 检查当前元素是否是类名或在类名附近
        PsiElement current = element;
        while (current != null && current != ktClass) {
            if (current == nameReference) {
                return true;
            }
            current = current.getParent();
        }

        return false;
    }

    /**
     * 判断光标是否在类声明行上
     *
     * <p>特殊处理类元素的定位，区分光标是在类声明行上
     * 还是在类内部。影响文档生成的范围。
     *
     * <p>判断逻辑：
     * <ul>
     *   <li>检查元素是否为类名标识符</li>
     *   <li>检查元素是否在类的修饰符列表中</li>
     *   <li>向上遍历元素树进行判断</li>
     * </ul>
     *
     * <p>使用场景：
     * <ul>
     *   <li>光标在类名上：只生成类的文档</li>
     *   <li>光标在类内部：生成整个类及所有成员的文档</li>
     * </ul>
     *
     * @param element  当前元素（光标位置的元素）
     * @param psiClass 类元素
     * @return 如果在类声明行上返回 true
     * @see PsiClass#getNameIdentifier()
     * @see PsiModifierList
     */
    private static boolean isOnClassDeclaration(@NotNull PsiElement element, @NotNull PsiClass psiClass) {
        // 获取类的标识符（类名）
        PsiIdentifier nameIdentifier = psiClass.getNameIdentifier();
        if (nameIdentifier == null) {
            return false;
        }

        // 检查当前元素是否是类名或在类名附近
        PsiElement current = element;
        while (current != null && current != psiClass) {
            if (current == nameIdentifier) {
                return true;
            }
            // 检查是否在类的修饰符列表中（如 public class）
            if (current.getParent() instanceof PsiModifierList &&
                current.getParent().getParent() == psiClass) {
                return true;
            }
            current = current.getParent();
        }

        return false;
    }

    /**
     * 检查元素是否已有 Javadoc/KDoc 注释
     *
     * <p>检查指定 PSI 元素是否已有文档注释（Javadoc 或 KDoc）。
     * 用于跳过已有文档的元素，避免重复生成。
     *
     * <p>检查条件：
     * <ul>
     *   <li>Java 元素：必须实现 PsiDocCommentOwner 接口，getDocComment() 方法不返回 null</li>
     *   <li>Kotlin 元素：检查是否有 KDoc 注释</li>
     * </ul>
     *
     * <p>线程安全：
     * <ul>
     *   <li>PSI 访问必须在 ReadAction 中执行</li>
     *   <li>该方法内部使用 ReadAction 保护，确保线程安全</li>
     *   <li>可以从任何线程安全调用</li>
     * </ul>
     *
     * @param element PSI 元素
     * @return 如果已有文档注释返回 true
     * @see PsiDocCommentOwner#getDocComment()
     */
    public static boolean hasJavaDoc(@NotNull PsiElement element) {
        // 使用 ReadAction 保护 PSI 访问，确保线程安全
        return ReadAction.compute(() -> {
            // Java 元素检查
            if (element instanceof PsiDocCommentOwner docOwner) {
                return docOwner.getDocComment() != null;
            }

            // Kotlin 元素检查
            if (element instanceof KtClassOrObject) {
                KDoc docComment = ((KtClassOrObject) element).getDocComment();
                return docComment != null;
            }
            if (element instanceof KtNamedFunction) {
                KDoc docComment = ((KtNamedFunction) element).getDocComment();
                return docComment != null;
            }
            if (element instanceof KtProperty) {
                KDoc docComment = ((KtProperty) element).getDocComment();
                return docComment != null;
            }

            return false;
        });
    }

    /**
     * 获取元素的简短描述（用于日志和提示）
     *
     * <p>生成元素的用户友好描述，用于日志记录和用户提示。
     * 包含元素类型和名称信息。
     *
     * <p>描述格式：
     * <ul>
     *   <li>方法: "方法: methodName()"</li>
     *   <li>字段: "字段: fieldName"</li>
     *   <li>类: "类: className"</li>
     *   <li>文件: "文件: fileName"</li>
     *   <li>其他: "元素: SimpleClassName"</li>
     * </ul>
     *
     * @param element PSI 元素
     * @return 元素描述字符串
     */
    @NotNull
    public static String getElementDescription(@NotNull PsiElement element) {
        if (element instanceof PsiMethod method) {
            return "方法: " + method.getName() + "()";
        } else if (element instanceof PsiField field) {
            return "字段: " + field.getName();
        } else if (element instanceof PsiClass psiClass) {
            return "类: " + psiClass.getName();
        } else if (element instanceof KtNamedFunction function) {
            return "函数: " + function.getName() + "()";
        } else if (element instanceof KtProperty property) {
            return "属性: " + property.getName();
        } else if (element instanceof KtClassOrObject ktClass) {
            return "类: " + ktClass.getName();
        } else if (element instanceof PsiFile) {
            return "文件: " + ((PsiFile) element).getName();
        } else {
            return "元素: " + element.getClass().getSimpleName();
        }
    }

    /**
     * 获取元素类型的中文描述
     *
     * <p>将定位类型枚举转换为中文描述字符串。
     * 用于用户界面显示和日志记录。
     *
     * <p>映射关系：
     * <ul>
     *   <li>METHOD → "方法"</li>
     *   <li>FIELD → "字段"</li>
     *   <li>CLASS → "类"</li>
     *   <li>FILE → "文件"</li>
     *   <li>默认 → "未知"</li>
     * </ul>
     *
     * @param type 定位类型枚举
     * @return 类型的中文描述
     */
    @NotNull
    public static String getTypeDescription(@NotNull LocateType type) {
        return switch (type) {
            case METHOD -> "方法";
            case FIELD -> "字段";
            case CLASS -> "类";
            case FILE -> "文件";
        };
    }
}

