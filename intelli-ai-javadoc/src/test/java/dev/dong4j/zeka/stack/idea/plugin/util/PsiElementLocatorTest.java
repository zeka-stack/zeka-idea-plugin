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
import com.intellij.psi.javadoc.PsiDocComment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PsiElementLocator 单元测试类
 * <p>
 * 该类用于对 PsiElementLocator 类的功能进行单元测试，包括对 LocateResult 的创建、类型判断、相等性比较、toString 方法以及元素描述生成等功能的验证。
 * <p>
 * 测试内容涵盖：
 * - 定位非 Java 文件时返回 null 的情况
 * - 定位偏移量处无元素时返回文件
 * - 检查元素是否包含 JavaDoc 注释
 * - 获取不同类型的元素描述（方法、字段、类、文件等）
 * - 获取 LocateType 枚举的类型描述
 * - LocateResult 的各种方法测试
 * - LocateResult 的相等性判断
 * - LocateResult 的类型检查
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@DisplayName("PsiElementLocator 单元测试")
public class PsiElementLocatorTest {

    /** 模拟的 Editor 对象，用于单元测试中替代真实 Editor 实例 */
    @Mock
    private Editor mockEditor;

    /** 模拟的 PsiJavaFile 对象，用于单元测试 */
    @Mock
    private PsiJavaFile mockPsiJavaFile;

    /** 模拟的非 Java 文件对象，用于测试场景 */
    @Mock
    private PsiFile mockNonJavaFile;

    /** 模拟的 PsiMethod 对象，用于单元测试中模拟方法行为 */
    @Mock
    private PsiMethod mockMethod;

    /** 模拟的 PsiField 对象，用于单元测试中模拟字段相关行为 */
    @Mock
    private PsiField mockField;

    /** 模拟的 PsiClass 对象，用于单元测试中的类相关操作 */
    @Mock
    private PsiClass mockClass;

    /** 模拟的 PsiElement 对象，用于单元测试 */
    @Mock
    private PsiElement mockElement;

    /** 模拟的 PsiIdentifier 对象，用于单元测试 */
    @Mock
    private PsiIdentifier mockIdentifier;

    /** 模拟的 PsiModifierList 对象，用于单元测试中的 mock 操作 */
    @Mock
    private PsiModifierList mockModifierList;

    /** 模拟的 PsiDocComment 对象，用于测试相关功能 */
    @Mock
    private PsiDocComment mockDocComment;

    /**
     * 初始化测试环境，设置Mockito注解
     * <p>
     * 用于在每个测试方法执行前初始化Mock对象，确保测试环境的稳定性
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试定位非 Java 文件时返回 null 的情况
     * <p>
     * 测试场景：当尝试定位一个非 Java 文件时
     * 预期结果：应返回 null
     * <p>
     * 说明：该测试模拟了一个编辑器环境，设置其光标位置，并尝试定位一个非 Java 文件，验证返回结果是否为 null
     */
    @Test
    @DisplayName("测试定位非 Java 文件返回 null")
    void testLocateElement_nonJavaFile() {
        when(mockEditor.getCaretModel()).thenReturn(mock(com.intellij.openapi.editor.CaretModel.class));
        when(mockEditor.getCaretModel().getOffset()).thenReturn(100);

        PsiElementLocator.LocateResult result = PsiElementLocator.locateElement(mockEditor, mockNonJavaFile);

        assertThat(result).isNull();
    }

    /**
     * 测试定位偏移量处无元素时返回文件
     * <p>
     * 测试场景：当在指定偏移量位置没有找到元素时
     * 预期结果：应返回一个包含文件信息的 LocateResult 对象
     * <p>
     * 验证点包括：结果不为空、元素为文件、类型为 FILE、isWholeFile() 为 true
     */
    @Test
    @DisplayName("测试定位偏移量处无元素时返回文件")
    void testLocateElementAtOffset_noElementAtOffset() {
        when(mockPsiJavaFile.findElementAt(100)).thenReturn(null);

        PsiElementLocator.LocateResult result = PsiElementLocator.locateElementAtOffset(mockPsiJavaFile, 100);

        assertThat(result).isNotNull();
        assertThat(result.element()).isEqualTo(mockPsiJavaFile);
        assertThat(result.type()).isEqualTo(PsiElementLocator.LocateType.FILE);
        assertThat(result.isWholeFile()).isTrue();
    }

    /**
     * 测试检查元素是否已有 JavaDoc 注释功能
     * <p>
     * 测试场景：当元素已包含 JavaDoc 注释时
     * 预期结果：应返回 true 表示元素具有 JavaDoc 注释
     * <p>
     * 该测试通过模拟 PsiDocCommentOwner 对象并设置其 getDocComment 方法返回一个 mock 的 JavaDoc 注释对象
     * 来验证 hasJavaDoc 方法的正确性
     */
    @Test
    @DisplayName("测试检查元素是否已有 JavaDoc - 有注释")
    void testHasJavaDoc_withDocComment() {
        PsiDocCommentOwner mockDocOwner = mock(PsiDocCommentOwner.class);
        when(mockDocOwner.getDocComment()).thenReturn(mockDocComment);

        boolean result = PsiElementLocator.hasJavaDoc(mockDocOwner);

        assertThat(result).isTrue();
    }

    /**
     * 测试检查元素是否已有 JavaDoc 注释功能
     * <p>
     * 测试场景：当元素没有 JavaDoc 注释时
     * 预期结果：应返回 false
     * <p>
     * 该测试用于验证 PsiElementLocator.hasJavaDoc 方法在元素无注释情况下的行为
     */
    @Test
    @DisplayName("测试检查元素是否已有 JavaDoc - 无注释")
    void testHasJavaDoc_withoutDocComment() {
        PsiDocCommentOwner mockDocOwner = mock(PsiDocCommentOwner.class);
        when(mockDocOwner.getDocComment()).thenReturn(null);

        boolean result = PsiElementLocator.hasJavaDoc(mockDocOwner);

        assertThat(result).isFalse();
    }

    /**
     * 测试检查元素是否已有 JavaDoc 注释的方法
     * <p>
     * 测试场景：当元素不是 DocCommentOwner 类型时
     * 预期结果：返回 false，表示该元素不支持 JavaDoc 注释
     * <p>
     * 说明：此测试用于验证 PsiElementLocator.hasJavaDoc 方法在非 DocCommentOwner 元素上的行为
     */
    @Test
    @DisplayName("测试检查元素是否已有 JavaDoc - 非 DocCommentOwner")
    void testHasJavaDoc_notDocCommentOwner() {
        boolean result = PsiElementLocator.hasJavaDoc(mockElement);

        assertThat(result).isFalse();
    }

    /**
     * 测试获取元素描述功能
     * <p>
     * 测试场景：当方法名为 getUserName 时
     * 预期结果：应返回描述 "方法: getUserName()"
     * <p>
     * 该测试验证 PsiElementLocator.getElementDescription 方法是否能正确解析并返回方法的描述信息
     */
    @Test
    @DisplayName("测试获取元素描述 - 方法")
    void testGetElementDescription_method() {
        when(mockMethod.getName()).thenReturn("getUserName");

        String description = PsiElementLocator.getElementDescription(mockMethod);

        assertThat(description).isEqualTo("方法: getUserName()");
    }

    /**
     * 测试获取元素描述功能 - 字段类型
     * <p>
     * 测试场景：模拟字段对象，其名称为 "username"
     * 预期结果：应返回描述 "字段: username"
     * <p>
     * 该测试验证 PsiElementLocator.getElementDescription 方法在处理字段类型时
     * 能正确拼接并返回字段名称的描述信息
     */
    @Test
    @DisplayName("测试获取元素描述 - 字段")
    void testGetElementDescription_field() {
        when(mockField.getName()).thenReturn("username");

        String description = PsiElementLocator.getElementDescription(mockField);

        assertThat(description).isEqualTo("字段: username");
    }

    /**
     * 测试获取元素描述功能 - 类类型
     * <p>
     * 测试场景：当传入一个类对象时
     * 预期结果：应返回格式为“类: 类名”的描述字符串
     */
    @Test
    @DisplayName("测试获取元素描述 - 类")
    void testGetElementDescription_class() {
        when(mockClass.getName()).thenReturn("UserService");

        String description = PsiElementLocator.getElementDescription(mockClass);

        assertThat(description).isEqualTo("类: UserService");
    }

    /**
     * 测试获取元素描述功能（文件类型）
     * <p>
     * 测试场景：模拟一个 PsiJavaFile 对象，其名称为 "UserService.java"
     * 预期结果：应返回描述信息 "文件: UserService.java"
     * <p>
     * 该测试验证 PsiElementLocator 在处理文件类型元素时，能否正确生成对应的描述信息
     */
    @Test
    @DisplayName("测试获取元素描述 - 文件")
    void testGetElementDescription_file() {
        when(mockPsiJavaFile.getName()).thenReturn("UserService.java");

        String description = PsiElementLocator.getElementDescription(mockPsiJavaFile);

        assertThat(description).isEqualTo("文件: UserService.java");
    }

    /**
     * 测试获取元素描述功能
     * <p>
     * 测试场景：当传入的元素为其他类型的 PsiElement 时
     * 预期结果：返回的描述字符串应包含 "元素:" 和 "PsiElement" 的信息
     * <p>
     * 该测试用于验证 PsiElementLocator.getElementDescription 方法在处理非特定类型元素时的描述生成逻辑
     */
    @Test
    @DisplayName("测试获取元素描述 - 其他元素")
    void testGetElementDescription_otherElement() {
        String description = PsiElementLocator.getElementDescription(mockElement);

        assertThat(description).contains("元素:");
        assertThat(description).contains("PsiElement");
    }

    /**
     * 测试获取类型描述功能 - METHOD
     * <p>
     * 测试场景：获取 PsiElementLocator.LocateType.METHOD 对应的类型描述
     * 预期结果：返回的描述应为 "方法"
     * <p>
     * 该测试验证 PsiElementLocator 类中 LocateType 枚举的 METHOD 成员对应的描述是否正确
     */
    @Test
    @DisplayName("测试获取类型描述 - METHOD")
    void testGetTypeDescription_method() {
        String description = PsiElementLocator.getTypeDescription(PsiElementLocator.LocateType.METHOD);

        assertThat(description).isEqualTo("方法");
    }

    /**
     * 测试获取类型描述功能 - FIELD 类型
     * <p>
     * 测试场景：获取 PsiElementLocator 中 FIELD 类型的描述
     * 预期结果：返回的描述应为 "字段"
     * <p>
     * 该测试验证 PsiElementLocator 类中 FIELD 类型对应的描述是否正确
     */
    @Test
    @DisplayName("测试获取类型描述 - FIELD")
    void testGetTypeDescription_field() {
        String description = PsiElementLocator.getTypeDescription(PsiElementLocator.LocateType.FIELD);

        assertThat(description).isEqualTo("字段");
    }

    /**
     * 测试获取类型描述功能 - CLASS 类型
     * <p>
     * 测试场景：请求获取 CLASS 类型的描述信息
     * 预期结果：返回的描述应为 "类"
     * <p>
     * 注意：该测试依赖 PsiElementLocator 类的静态方法 LocateType.CLASS
     */
    @Test
    @DisplayName("测试获取类型描述 - CLASS")
    void testGetTypeDescription_class() {
        String description = PsiElementLocator.getTypeDescription(PsiElementLocator.LocateType.CLASS);

        assertThat(description).isEqualTo("类");
    }

    /**
     * 测试获取类型描述功能 - FILE
     * <p>
     * 测试场景：验证通过 FILE 类型获取对应的描述信息
     * 预期结果：返回的描述应为 "文件"
     * <p>
     * 注意：该测试依赖 PsiElementLocator 类的实现，确保其能正确解析 FILE 类型
     */
    @Test
    @DisplayName("测试获取类型描述 - FILE")
    void testGetTypeDescription_file() {
        String description = PsiElementLocator.getTypeDescription(PsiElementLocator.LocateType.FILE);

        assertThat(description).isEqualTo("文件");
    }

    /**
     * 测试 LocateResult 类的方法
     * <p>
     * 测试场景：创建 LocateResult 实例并验证其各个属性值
     * 预期结果：isMethod() 返回 true，其余 isXXX() 方法返回 false
     * <p>
     * 该测试用于验证 LocateResult 构造方法是否正确初始化了各个字段
     */
    @Test
    @DisplayName("测试 LocateResult 的方法")
    void testLocateResult_methods() {
        PsiElementLocator.LocateResult result = new PsiElementLocator.LocateResult(
            mockMethod,
            PsiElementLocator.LocateType.METHOD,
            false
        );

        assertThat(result.isMethod()).isTrue();
        assertThat(result.isField()).isFalse();
        assertThat(result.isClass()).isFalse();
        assertThat(result.isWholeFile()).isFalse();
    }

    /**
     * 测试 LocateResult 的字段定位功能
     * <p>
     * 测试场景：创建一个表示字段定位的 LocateResult 实例
     * 预期结果：验证 LocateResult 是否正确识别为字段类型
     * <p>
     * 测试过程中创建了一个模拟字段对象，并设置 LocateType 为 FIELD，用于验证 isField() 方法的返回值
     */
    @Test
    @DisplayName("测试 LocateResult - Field")
    void testLocateResult_field() {
        PsiElementLocator.LocateResult result = new PsiElementLocator.LocateResult(
            mockField,
            PsiElementLocator.LocateType.FIELD,
            false
        );

        assertThat(result.isMethod()).isFalse();
        assertThat(result.isField()).isTrue();
        assertThat(result.isClass()).isFalse();
    }

    /**
     * 测试 LocateResult 类的创建和属性判断
     * <p>
     * 测试场景：验证 LocateResult 实例是否正确初始化，并检查其 isClass() 和 isWholeFile() 方法返回值
     * 预期结果：isClass() 应返回 true，isWholeFile() 应返回 true，而 isMethod() 和 isField() 应返回 false
     * <p>
     * 说明：该测试用于确保 LocateResult 枚举的各个属性能够正确区分不同的定位类型
     */
    @Test
    @DisplayName("测试 LocateResult - Class")
    void testLocateResult_class() {
        PsiElementLocator.LocateResult result = new PsiElementLocator.LocateResult(
            mockClass,
            PsiElementLocator.LocateType.CLASS,
            true
        );

        assertThat(result.isMethod()).isFalse();
        assertThat(result.isField()).isFalse();
        assertThat(result.isClass()).isTrue();
        assertThat(result.isWholeFile()).isTrue();
    }

    /**
     * 测试 LocateResult 类的 toString 方法
     * <p>
     * 测试场景：验证 LocateResult 对象的 toString 方法是否正确地将对象信息格式化为字符串
     * 预期结果：生成的字符串应包含 "LocateResult"、"METHOD" 和 "false" 关键字
     * <p>
     * 说明：测试中使用了 mock 对象模拟 getClass() 方法返回 PsiMethod 类型，以确保 toString 方法能正确反映对象状态
     */
    @Test
    @DisplayName("测试 LocateResult toString")
    void testLocateResult_toString() {
        when(mockMethod.getClass()).thenReturn((Class) PsiMethod.class);

        PsiElementLocator.LocateResult result = new PsiElementLocator.LocateResult(
            mockMethod,
            PsiElementLocator.LocateType.METHOD,
            false
        );

        String toString = result.toString();
        assertThat(toString).contains("LocateResult");
        assertThat(toString).contains("METHOD");
        assertThat(toString).contains("false");
    }

    /**
     * 测试 LocateType 枚举的所有值
     * <p>
     * 测试场景：验证 PsiElementLocator.LocateType 枚举是否包含预定义的四种类型
     * 预期结果：枚举值应包含 METHOD、FIELD、CLASS 和 FILE
     */
    @Test
    @DisplayName("测试 LocateType 枚举所有值")
    void testLocateType_allValues() {
        PsiElementLocator.LocateType[] types = PsiElementLocator.LocateType.values();

        assertThat(types).contains(
            PsiElementLocator.LocateType.METHOD,
            PsiElementLocator.LocateType.FIELD,
            PsiElementLocator.LocateType.CLASS,
            PsiElementLocator.LocateType.FILE
                                  );
    }

    /**
     * 测试 LocateResult 的 getter 方法
     * <p>
     * 测试场景：创建一个 LocateResult 实例并验证其 getter 方法的返回值
     * 预期结果：element() 应返回传入的 mockMethod，type() 应返回 LocateType.METHOD，isWholeFile() 应返回 false
     */
    @Test
    @DisplayName("测试 LocateResult getter 方法")
    void testLocateResult_getters() {
        PsiElementLocator.LocateResult result = new PsiElementLocator.LocateResult(
            mockMethod,
            PsiElementLocator.LocateType.METHOD,
            false
        );

        assertThat(result.element()).isEqualTo(mockMethod);
        assertThat(result.type()).isEqualTo(PsiElementLocator.LocateType.METHOD);
        assertThat(result.isWholeFile()).isFalse();
    }

    /**
     * 测试获取元素描述功能，当方法名为 null 时
     * <p>
     * 测试场景：方法名返回 null
     * 预期结果：应返回 "方法: null()" 作为描述
     * <p>
     * 注意：此测试依赖于 PsiElementLocator.getElementDescription 方法的实现逻辑
     */
    @Test
    @DisplayName("测试获取元素描述 - 方法名为 null")
    void testGetElementDescription_methodWithNullName() {
        when(mockMethod.getName()).thenReturn(null);

        String description = PsiElementLocator.getElementDescription(mockMethod);

        assertThat(description).isEqualTo("方法: null()");
    }

    /**
     * 测试获取元素描述功能，字段名为 null 的场景
     * <p>
     * 测试场景：当字段名返回 null 时
     * 预期结果：应返回 "字段: null" 的描述信息
     * <p>
     * 注意：该测试依赖 mock 对象 mockField，需确保其 getName() 方法返回 null
     */
    @Test
    @DisplayName("测试获取元素描述 - 字段名为 null")
    void testGetElementDescription_fieldWithNullName() {
        when(mockField.getName()).thenReturn(null);

        String description = PsiElementLocator.getElementDescription(mockField);

        assertThat(description).isEqualTo("字段: null");
    }

    /**
     * 测试获取元素描述功能，当类名为 null 时的处理情况
     * <p>
     * 测试场景：传入的类对象的 getName() 方法返回 null
     * 预期结果：返回的描述应为 "类: null"
     * <p>
     * 该测试验证了在类名为空的情况下，获取元素描述功能是否能正确处理并返回预期结果
     */
    @Test
    @DisplayName("测试获取元素描述 - 类名为 null")
    void testGetElementDescription_classWithNullName() {
        when(mockClass.getName()).thenReturn(null);

        String description = PsiElementLocator.getElementDescription(mockClass);

        assertThat(description).isEqualTo("类: null");
    }

    /**
     * 测试 LocateResult 对象的相等性
     * <p>
     * 测试场景：创建两个具有相同参数的 LocateResult 实例
     * 预期结果：两个实例应被视为相等
     * <p>
     * 说明：Record 类会自动实现 equals 方法，因此无需手动覆盖
     */
    @Test
    @DisplayName("测试 LocateResult 相等性")
    void testLocateResult_equality() {
        PsiElementLocator.LocateResult result1 = new PsiElementLocator.LocateResult(
            mockMethod,
            PsiElementLocator.LocateType.METHOD,
            false
        );

        PsiElementLocator.LocateResult result2 = new PsiElementLocator.LocateResult(
            mockMethod,
            PsiElementLocator.LocateType.METHOD,
            false
        );

        // Record 类会自动实现 equals
        assertThat(result1).isEqualTo(result2);
    }

    /**
     * 测试 LocateResult 对象不相等的情况
     * <p>
     * 测试场景：两个 LocateResult 对象具有相同的 PsiElement 和 LocateType，但 isWholeFile 属性不同
     * 预期结果：两个对象应被视为不相等
     * <p>
     * 该测试验证 LocateResult 类的 equals 方法是否正确地根据 isWholeFile 属性判断对象相等性
     */
    @Test
    @DisplayName("测试 LocateResult 不相等")
    void testLocateResult_notEqual() {
        PsiElementLocator.LocateResult result1 = new PsiElementLocator.LocateResult(
            mockMethod,
            PsiElementLocator.LocateType.METHOD,
            false
        );

        PsiElementLocator.LocateResult result2 = new PsiElementLocator.LocateResult(
            mockMethod,
            PsiElementLocator.LocateType.METHOD,
            true  // 不同的 isWholeFile
        );

        assertThat(result1).isNotEqualTo(result2);
    }

    /**
     * 测试 LocateResult 类中所有类型的判断方法
     * <p>
     * 测试场景：分别创建表示方法、字段、类和文件的 LocateResult 实例
     * 预期结果：每个实例应能正确识别其对应的类型
     * <p>
     * 注意：对于文件类型，由于没有 isFile() 方法，只能通过 type() 方法判断类型
     */
    @Test
    @DisplayName("测试所有类型的判断方法")
    void testLocateResult_allTypeChecks() {
        // Method
        PsiElementLocator.LocateResult methodResult = new PsiElementLocator.LocateResult(
            mockMethod, PsiElementLocator.LocateType.METHOD, false
        );
        assertThat(methodResult.isMethod()).isTrue();

        // Field
        PsiElementLocator.LocateResult fieldResult = new PsiElementLocator.LocateResult(
            mockField, PsiElementLocator.LocateType.FIELD, false
        );
        assertThat(fieldResult.isField()).isTrue();

        // Class
        PsiElementLocator.LocateResult classResult = new PsiElementLocator.LocateResult(
            mockClass, PsiElementLocator.LocateType.CLASS, false
        );
        assertThat(classResult.isClass()).isTrue();

        // File - 没有 isFile() 方法，只能通过 type() 判断
        PsiElementLocator.LocateResult fileResult = new PsiElementLocator.LocateResult(
            mockPsiJavaFile, PsiElementLocator.LocateType.FILE, true
        );
        assertThat(fileResult.type()).isEqualTo(PsiElementLocator.LocateType.FILE);
    }
}

