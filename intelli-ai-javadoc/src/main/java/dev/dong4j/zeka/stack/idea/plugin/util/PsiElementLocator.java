package dev.dong4j.zeka.stack.idea.plugin.util;

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

import lombok.extern.slf4j.Slf4j;

/**
 * PSI 元素定位器
 * <p>根据编辑器光标位置智能定位需要生成文档的 PSI 元素
 *
 * <p>作为插件智能识别功能的核心组件，负责根据用户光标位置
 * 准确识别需要生成文档的代码元素，提供精准的上下文感知。
 *
 * <p>定位优先级：
 * <ol>
 *   <li>方法 (PsiMethod)：光标在方法内部或方法声明上</li>
 *   <li>字段 (PsiField)：光标在字段声明上</li>
 *   <li>类 (PsiClass)：光标在类声明上或类内部</li>
 *   <li>整个文件 (PsiFile)：无法定位到具体元素时</li>
 * </ol>
 *
 * <p>核心功能：
 * <ul>
 *   <li>精确的元素定位算法</li>
 *   <li>智能的上下文识别</li>
 *   <li>类声明行的特殊处理</li>
 *   <li>友好的元素描述生成</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class PsiElementLocator {

    /**
     * 定位结果
     *
     * <p>封装元素定位的结果信息，包含定位到的元素、元素类型
     * 以及是否需要为整个文件或类生成文档的标志。
     *
     * <p>设计考虑：
     * <ul>
     *   <li>使用 record 简化代码，提高可读性</li>
     *   <li>包含完整的定位信息，便于后续处理</li>
     *   <li>提供便捷的类型检查方法</li>
     * </ul>
     *
     * @param element     定位到的 PSI 元素
     * @param type        定位类型（方法、字段、类、文件）
     * @param isWholeFile 是否需要为整个文件生成（类内部时为 true）
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
     * 定位类型
     *
     * <p>定义了支持的元素定位类型，用于区分不同的处理逻辑。
     * 每种类型对应不同的文档生成策略和范围。
     *
     * <p>类型说明：
     * <ul>
     *   <li>METHOD：方法级别，只处理单个方法</li>
     *   <li>FIELD：字段级别，只处理单个字段</li>
     *   <li>CLASS：类级别，可能处理整个类或单个类</li>
     *   <li>FILE：文件级别，处理整个文件</li>
     * </ul>
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
        if (!(psiFile instanceof PsiJavaFile)) {
            log.debug("Not a Java file: {}", psiFile.getName());
            return null;
        }

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
     * 检查元素是否已有 JavaDoc 注释
     *
     * <p>检查指定 PSI 元素是否已有 JavaDoc 注释。
     * 用于跳过已有文档的元素，避免重复生成。
     *
     * <p>检查条件：
     * <ul>
     *   <li>元素必须实现 PsiDocCommentOwner 接口</li>
     *   <li>元素的 getDocComment() 方法不返回 null</li>
     * </ul>
     *
     * @param element PSI 元素
     * @return 如果已有 JavaDoc 返回 true
     * @see PsiDocCommentOwner#getDocComment()
     */
    public static boolean hasJavaDoc(@NotNull PsiElement element) {
        if (element instanceof PsiDocCommentOwner docOwner) {
            return docOwner.getDocComment() != null;
        }
        return false;
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

