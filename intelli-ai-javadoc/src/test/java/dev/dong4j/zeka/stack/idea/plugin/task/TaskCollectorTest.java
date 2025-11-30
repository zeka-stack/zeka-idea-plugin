package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.javadoc.PsiDocComment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * TaskCollector 单元测试类
 * <p>
 * 该类用于对 TaskCollector 类进行单元测试，验证其收集文档任务的功能是否符合预期。
 * 包括对方法、测试方法、字段、类等元素的收集逻辑进行测试，同时验证配置参数对任务生成的影响。
 * 还测试了跳过已有文档、文件类型判断、目录处理等场景。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@DisplayName("TaskCollector 单元测试")
public class TaskCollectorTest {

    /** 模拟的 Project 对象，用于单元测试 */
    @Mock
    private Project mockProject;

    /** 模拟的 PsiManager 实例，用于单元测试 */
    @Mock
    private PsiManager mockPsiManager;

    /** 模拟的 PsiJavaFile 对象，用于单元测试 */
    @Mock
    private PsiJavaFile mockPsiJavaFile;

    /** 模拟的 PsiMethod 对象，用于单元测试中模拟方法行为 */
    @Mock
    private PsiMethod mockMethod;

    /** 模拟的测试方法对象，用于单元测试中模拟方法行为 */
    @Mock
    private PsiMethod mockTestMethod;

    /** 模拟的 PsiField 对象，用于单元测试中模拟字段相关行为 */
    @Mock
    private PsiField mockField;

    /** 模拟的 PsiClass 对象，用于单元测试中的类相关操作 */
    @Mock
    private PsiClass mockClass;

    /** 模拟的 PsiModifierList 对象，用于单元测试中模拟 PsiElement 的修饰符列表 */
    @Mock
    private PsiModifierList mockModifierList;

    /** JUnit4 注解模拟对象，用于测试中模拟 PsiAnnotation 行为 */
    @Mock
    private PsiAnnotation mockJUnit4Annotation;

    /** JUnit5 注解模拟对象，用于测试中模拟注解行为 */
    @Mock
    private PsiAnnotation mockJUnit5Annotation;

    /** 模拟的 PsiDocComment 对象，用于测试相关功能 */
    @Mock
    private PsiDocComment mockDocComment;

    /** 模拟的 VirtualFile 对象，用于测试 */
    @Mock
    private VirtualFile mockVirtualFile;

    /** 模拟的目录对象，用于测试文件系统相关操作 */
    @Mock
    private VirtualFile mockDirectory;

    /** 模拟的设置状态对象，用于测试场景下的配置状态模拟 */
    @Mock
    private SettingsState mockSettings;

    /** 模拟的 Application 对象，用于测试环境中替代真实应用实例 */
    @Mock
    private com.intellij.openapi.application.Application mockApplication;

    /** 任务收集器，用于收集和管理任务信息 */
    private TaskCollector taskCollector;
    /** 实际的设置状态对象 */
    private SettingsState realSettings;

    /**
     * 初始化测试环境，设置必要的 mock 对象和模拟数据
     * <p>
     * 该方法在每个测试用例执行前被调用，用于初始化 mock 对象、模拟数据以及配置测试环境。
     * 包括设置 ApplicationManager、SettingsState 的 mock 行为，以及为方法、测试方法、字段和类创建模拟对象。
     *
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 创建真实的 settings 用于测试
        realSettings = new SettingsState();
        realSettings.generateForClass = true;
        realSettings.generateForMethod = true;
        realSettings.generateForField = true;
        realSettings.overrideExisting = true;

        // Mock Application 和 SettingsState
        try (MockedStatic<ApplicationManager> mockedAppManager = mockStatic(ApplicationManager.class);
             MockedStatic<SettingsState> mockedSettings = mockStatic(SettingsState.class)) {

            mockedAppManager.when(ApplicationManager::getApplication).thenReturn(mockApplication);
            mockedSettings.when(SettingsState::getInstance).thenReturn(realSettings);

            taskCollector = new TaskCollector(mockProject);
        }

        // 基本的 mock 设置
        when(mockMethod.getText()).thenReturn("public void testMethod() {}");
        when(mockMethod.getName()).thenReturn("testMethod");
        when(mockMethod.getModifierList()).thenReturn(mockModifierList);
        when(mockMethod.getContainingFile()).thenReturn(mockPsiJavaFile);

        when(mockTestMethod.getText()).thenReturn("@Test public void testSomething() {}");
        when(mockTestMethod.getName()).thenReturn("testSomething");
        when(mockTestMethod.getModifierList()).thenReturn(mockModifierList);
        when(mockTestMethod.getContainingFile()).thenReturn(mockPsiJavaFile);

        when(mockField.getText()).thenReturn("private String username;");
        when(mockField.getName()).thenReturn("username");
        when(mockField.getContainingFile()).thenReturn(mockPsiJavaFile);

        when(mockClass.getText()).thenReturn("public class TestClass {}");
        when(mockClass.getName()).thenReturn("TestClass");
        when(mockClass.getMethods()).thenReturn(new PsiMethod[0]);
        when(mockClass.getFields()).thenReturn(new PsiField[0]);
        when(mockClass.getInnerClasses()).thenReturn(new PsiClass[0]);
        when(mockClass.getContainingFile()).thenReturn(mockPsiJavaFile);

        when(mockPsiJavaFile.getVirtualFile()).thenReturn(mockVirtualFile);
        when(mockVirtualFile.getPath()).thenReturn("/test/path/TestFile.java");
    }

    /**
     * 测试从单个方法收集任务的功能
     * <p>
     * 测试场景：模拟一个方法对象，验证任务收集器是否能正确收集到对应的任务
     * 预期结果：收集到的任务列表应包含一个类型为 METHOD 的任务，且其元素为传入的 mock 方法
     * <p>
     * 说明：该测试需要 mock 方法对象作为输入，确保任务收集器能够正确识别并收集方法级别的文档任务
     */
    @Test
    @DisplayName("测试从单个方法收集任务")
    void testCollectFromElement_method() {
        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockMethod);

        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        assertThat(task.getType()).isEqualTo(DocumentationTask.TaskType.METHOD);
        assertThat(task.getElement()).isEqualTo(mockMethod);
    }

    /**
     * 测试从测试方法收集任务功能（JUnit 4 情况）
     * <p>
     * 测试场景：模拟测试方法包含 JUnit 4 的 @Test 注解，不包含 JUnit 5 的 @Test 注解
     * 预期结果：应收集到一个类型为 TEST_METHOD 的任务
     * <p>
     * 说明：该测试需要 mockModifierList 返回对应的注解对象，以模拟真实环境中的注解查找行为
     */
    @Test
    @DisplayName("测试从测试方法收集任务 - JUnit 4")
    void testCollectFromElement_testMethod_JUnit4() {
        when(mockModifierList.findAnnotation("org.junit.Test")).thenReturn(mockJUnit4Annotation);
        when(mockModifierList.findAnnotation("org.junit.jupiter.api.Test")).thenReturn(null);

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockTestMethod);

        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        assertThat(task.getType()).isEqualTo(DocumentationTask.TaskType.TEST_METHOD);
    }

    /**
     * 测试从测试方法收集任务功能（JUnit 5）
     * <p>
     * 测试场景：模拟测试方法包含 JUnit 5 的 @Test 注解
     * 预期结果：应收集到一个类型为 TEST_METHOD 的任务
     * <p>
     * 说明：测试中使用了 Mockito 模拟 ModifierList 的 findAnnotation 方法，验证任务收集逻辑是否正确
     */
    @Test
    @DisplayName("测试从测试方法收集任务 - JUnit 5")
    void testCollectFromElement_testMethod_JUnit5() {
        when(mockModifierList.findAnnotation("org.junit.Test")).thenReturn(null);
        when(mockModifierList.findAnnotation("org.junit.jupiter.api.Test")).thenReturn(mockJUnit5Annotation);

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockTestMethod);

        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        assertThat(task.getType()).isEqualTo(DocumentationTask.TaskType.TEST_METHOD);
    }

    /**
     * 测试从单个字段收集任务的功能
     * <p>
     * 测试场景：模拟一个字段对象，验证任务收集器是否能正确收集到对应任务
     * 预期结果：收集到的任务列表应包含一个类型为 FIELD 的任务，且其元素为模拟的字段对象
     * <p>
     * 注意：测试中使用了 mockField 对象作为模拟字段，确保任务收集逻辑正确
     */
    @Test
    @DisplayName("测试从单个字段收集任务")
    void testCollectFromElement_field() {
        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockField);

        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        assertThat(task.getType()).isEqualTo(DocumentationTask.TaskType.FIELD);
        assertThat(task.getElement()).isEqualTo(mockField);
    }

    /**
     * 测试从类收集任务功能，验证空类的情况
     * <p>
     * 测试场景：当传入一个没有任何注解的空类时
     * 预期结果：应为该类生成一个类型为 {@link DocumentationTask.TaskType#CLASS} 的任务
     * <p>
     * 特殊说明：需要确保 mockClass 是一个没有任何注解的空类，以模拟真实场景
     */
    @Test
    @DisplayName("测试从类收集任务 - 空类")
    void testCollectFromElement_emptyClass() {
        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockClass);

        // 应该只为类本身生成一个任务
        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        assertThat(task.getType()).isEqualTo(DocumentationTask.TaskType.CLASS);
    }

    /**
     * 测试从类收集任务功能
     * <p>
     * 测试场景：当类包含方法和字段时
     * 预期结果：应为类、方法和字段各生成一个任务，总任务数为3
     * <p>
     * 该测试模拟了一个包含方法和字段的类，并验证任务收集器是否能正确识别并生成对应类型的任务
     */
    @Test
    @DisplayName("测试从类收集任务 - 包含方法和字段")
    void testCollectFromElement_classWithMembers() {
        when(mockClass.getMethods()).thenReturn(new PsiMethod[] {mockMethod});
        when(mockClass.getFields()).thenReturn(new PsiField[] {mockField});

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockClass);

        // 应该为类、方法和字段各生成一个任务
        assertThat(tasks).hasSize(3);
        assertThat(tasks).anyMatch(t -> t.getType() == DocumentationTask.TaskType.CLASS);
        assertThat(tasks).anyMatch(t -> t.getType() == DocumentationTask.TaskType.METHOD);
        assertThat(tasks).anyMatch(t -> t.getType() == DocumentationTask.TaskType.FIELD);
    }

    /**
     * 测试收集文档任务时跳过已有文档的元素
     * <p>
     * 测试场景：当设置跳过已有文档时，收集文档任务
     * 预期结果：应返回空列表，表示已跳过已有文档的方法
     * <p>
     * 说明：该测试模拟了一个已有文档的 PsiMethod 对象，并验证任务收集器是否能正确识别并跳过该方法
     */
    @Test
    @DisplayName("测试跳过已有文档的元素")
    void testCollectFromElement_skipExisting() {
        realSettings.overrideExisting = false;
        // Mock 方法已有文档
        PsiDocCommentOwner methodWithDoc = mock(PsiDocCommentOwner.class, withSettings()
            .extraInterfaces(PsiMethod.class));
        when(methodWithDoc.getDocComment()).thenReturn(mockDocComment);
        when(methodWithDoc.getText()).thenReturn("public void test() {}");
        when(((PsiMethod) methodWithDoc).getModifierList()).thenReturn(mockModifierList);
        when(methodWithDoc.getContainingFile()).thenReturn(mockPsiJavaFile);

        List<DocumentationTask> tasks = taskCollector.collectFromElement(methodWithDoc);

        // 应该跳过已有文档的方法
        assertThat(tasks).isEmpty();
    }

    /**
     * 测试从元素收集文档任务的功能
     * <p>
     * 测试场景：当不跳过已有文档的元素时
     * 预期结果：应收集到一个文档任务
     * <p>
     * 该测试验证在设置 skipExisting 为 false 的情况下，collectFromElement 方法不会跳过已包含文档注释的元素。
     * 通过模拟一个具有已有文档注释的 PsiDocCommentOwner 对象，确保任务收集器能够正确识别并包含该元素。
     */
    @Test
    @DisplayName("测试不跳过已有文档的元素")
    void testCollectFromElement_notSkipExisting() {
        realSettings.overrideExisting = true;

        // Mock 方法已有文档
        PsiDocCommentOwner methodWithDoc = mock(PsiDocCommentOwner.class, withSettings()
            .extraInterfaces(PsiMethod.class));
        when(methodWithDoc.getDocComment()).thenReturn(mockDocComment);
        when(methodWithDoc.getText()).thenReturn("public void test() {}");
        when(((PsiMethod) methodWithDoc).getModifierList()).thenReturn(mockModifierList);
        when(methodWithDoc.getContainingFile()).thenReturn(mockPsiJavaFile);

        List<DocumentationTask> tasks = taskCollector.collectFromElement(methodWithDoc);

        // 不应该跳过
        assertThat(tasks).hasSize(1);
    }

    /**
     * 测试配置禁用某类型时不生成任务
     * <p>
     * 测试场景：当配置禁用方法类型时
     * 预期结果：任务收集器应返回空列表
     */
    @Test
    @DisplayName("测试配置禁用某类型时不生成任务 - 方法")
    void testCollectFromElement_methodDisabled() {
        realSettings.generateForMethod = false;

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockMethod);

        assertThat(tasks).isEmpty();
    }

    /**
     * 测试配置禁用某类型时不生成任务
     * <p>
     * 测试场景：当字段类型被设置为不生成任务时
     * 预期结果：任务收集器应返回空列表
     * <p>
     * 说明：通过设置 realSettings.generateForField 为 false 模拟禁用状态，验证任务收集逻辑是否正确处理该情况
     */
    @Test
    @DisplayName("测试配置禁用某类型时不生成任务 - 字段")
    void testCollectFromElement_fieldDisabled() {
        realSettings.generateForField = false;

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockField);

        assertThat(tasks).isEmpty();
    }

    /**
     * 测试配置禁用某类型时不生成任务 - 类
     * <p>
     * 测试场景：当配置中禁用类类型时，检查是否不会生成类类型的文档任务
     * 预期结果：任务列表中不应包含类型为CLASS的任务
     * <p>
     * 说明：通过设置realSettings.generateForClass为false，模拟禁用类类型生成的配置，验证任务收集器是否正确过滤掉此类任务
     */
    @Test
    @DisplayName("测试配置禁用某类型时不生成任务 - 类")
    void testCollectFromElement_classDisabled() {
        realSettings.generateForClass = false;

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockClass);

        // 类本身被禁用，但可能有方法和字段
        assertThat(tasks).noneMatch(t -> t.getType() == DocumentationTask.TaskType.CLASS);
    }

    /**
     * 测试从文件收集任务功能，用于验证非 Java 文件不会产生任何任务
     * <p>
     * 测试场景：传入一个非 Java 类型的 PsiFile 对象
     * 预期结果：任务收集器应返回一个空列表
     * <p>
     * 该测试用于确保任务收集逻辑能够正确识别并忽略非 Java 文件
     */
    @Test
    @DisplayName("测试从文件收集任务 - 非 Java 文件")
    void testCollectFromFile_nonJavaFile() {
        PsiFile mockNonJavaFile = mock(PsiFile.class);

        List<DocumentationTask> tasks = taskCollector.collectFromFile(mockNonJavaFile);

        assertThat(tasks).isEmpty();
    }

    /**
     * 测试从虚拟文件收集任务功能，文件不存在的场景
     * <p>
     * 测试场景：模拟虚拟文件不存在，调用 PsiManager 查找文件返回 null
     * 预期结果：任务收集结果应为空列表
     * <p>
     * 注意：测试中使用了 Mockito 的 mockStatic 方法对 PsiManager 进行静态方法模拟
     */
    @Test
    @DisplayName("测试从虚拟文件收集任务 - 文件不存在")
    void testCollectFromVirtualFile_fileNotFound() {
        when(mockPsiManager.findFile(mockVirtualFile)).thenReturn(null);

        try (MockedStatic<PsiManager> mockedPsiManager = mockStatic(PsiManager.class)) {
            mockedPsiManager.when(() -> PsiManager.getInstance(mockProject))
                .thenReturn(mockPsiManager);

            List<DocumentationTask> tasks = taskCollector.collectFromVirtualFile(mockVirtualFile);

            assertThat(tasks).isEmpty();
        }
    }

    /**
     * 测试从目录收集任务功能，模拟空目录场景
     * <p>
     * 测试场景：当传入的目录为空目录时
     * 预期结果：应返回一个空的任务列表
     * <p>
     * 说明：通过 mock 对象模拟目录对象，设置其 isDirectory 方法返回 true，getChildren 方法返回空数组
     */
    @Test
    @DisplayName("测试从目录收集任务 - 空目录")
    void testCollectFromDirectory_emptyDirectory() {
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.getChildren()).thenReturn(new VirtualFile[0]);

        List<DocumentationTask> tasks = taskCollector.collectFromDirectory(mockDirectory);

        assertThat(tasks).isEmpty();
    }

    /**
     * 测试从目录收集任务功能，验证非目录情况
     * <p>
     * 测试场景：传入的路径不是一个目录
     * 预期结果：收集到的任务列表应为空
     * <p>
     * 说明：通过 mock 对象模拟 isDirectory 方法返回 false，验证任务收集逻辑是否正确处理非目录情况
     */
    @Test
    @DisplayName("测试从目录收集任务 - 非目录")
    void testCollectFromDirectory_notDirectory() {
        when(mockDirectory.isDirectory()).thenReturn(false);

        List<DocumentationTask> tasks = taskCollector.collectFromDirectory(mockDirectory);

        assertThat(tasks).isEmpty();
    }

    /**
     * 测试任务是否包含正确的代码内容
     * <p>
     * 测试场景：模拟方法返回预设的代码字符串，验证任务收集器是否正确提取代码
     * 预期结果：任务列表应包含一个元素，且该任务的代码内容与预期一致
     * <p>
     * 该测试依赖 {@link DocumentationTask} 和 {@link TaskCollectorTest#taskCollector} 的正确实现
     */
    @Test
    @DisplayName("测试任务包含正确的代码内容")
    void testTask_hasCorrectCode() {
        String expectedCode = "public void testMethod() {}";
        when(mockMethod.getText()).thenReturn(expectedCode);

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockMethod);

        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        assertThat(task.getCode()).isEqualTo(expectedCode);
    }

    /**
     * 测试任务包含正确的文件路径
     * <p>
     * 测试场景：验证从方法元素收集的任务是否包含预期的文件路径
     * 预期结果：任务列表应包含一个元素，其文件路径与预期路径一致
     * <p>
     * 注意：测试中使用了 mock 方法元素来模拟方法信息，确保路径收集逻辑正确
     */
    @Test
    @DisplayName("测试任务包含正确的文件路径")
    void testTask_hasCorrectFilePath() {
        String expectedPath = "/test/path/TestFile.java";

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockMethod);

        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        assertThat(task.getFilePath()).isEqualTo(expectedPath);
    }

    /**
     * 测试任务初始状态
     * <p>
     * 测试场景：当调用任务收集方法时，模拟方法元素被传入
     * 预期结果：应收集到一个状态为 PENDING 的任务
     * <p>
     * 注意：测试中使用了 mock 方法元素来模拟输入场景
     */
    @Test
    @DisplayName("测试任务初始状态")
    void testTask_initialStatus() {
        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockMethod);

        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.PENDING);
    }

    /**
     * 测试从 PsiClass 收集文档任务的功能，包含内部类的情况
     * <p>
     * 测试场景：模拟一个包含内部类的 PsiClass 对象，并验证任务收集器是否能正确识别并为外部类和内部类各生成一个文档任务
     * 预期结果：收集到的任务列表大小应大于等于 2，且包含外部类和内部类对应的元素
     * <p>
     * 注意：此测试需要 PsiClass 和 PsiMethod 等 mock 对象配合使用，以模拟代码结构和行为
     */
    @Test
    @DisplayName("测试从类收集任务 - 包含内部类")
    void testCollectFromElement_classWithInnerClass() {
        PsiClass mockInnerClass = mock(PsiClass.class);
        when(mockInnerClass.getText()).thenReturn("public static class InnerClass {}");
        when(mockInnerClass.getName()).thenReturn("InnerClass");
        when(mockInnerClass.getMethods()).thenReturn(new PsiMethod[0]);
        when(mockInnerClass.getFields()).thenReturn(new PsiField[0]);
        when(mockInnerClass.getInnerClasses()).thenReturn(new PsiClass[0]);
        when(mockInnerClass.getContainingFile()).thenReturn(mockPsiJavaFile);

        when(mockClass.getInnerClasses()).thenReturn(new PsiClass[] {mockInnerClass});

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockClass);

        // 应该为外部类和内部类各生成一个任务
        assertThat(tasks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(tasks).anyMatch(t -> t.getElement() == mockClass);
        assertThat(tasks).anyMatch(t -> t.getElement() == mockInnerClass);
    }

    /**
     * 测试从 PsiFile 收集任务功能
     * <p>
     * 测试场景：模拟 PsiJavaFile 对象，验证 collectFromElement 方法是否正确收集任务
     * 预期结果：应未调用 PsiJavaFile 的 accept 方法
     * <p>
     * 注意：测试中使用了 mock 对象，确保不会触发实际的 PsiFile 操作
     */
    @Test
    @DisplayName("测试从 PsiFile 收集任务")
    void testCollectFromElement_psiFile() {
        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockPsiJavaFile);

        // 调用 collectFromFile
        verify(mockPsiJavaFile, never()).accept(any()); // 验证是否调用了 visitor
    }

    /**
     * 测试所有配置都禁用时不生成任务
     * <p>
     * 测试场景：当 generateForClass、generateForMethod 和 generateForField 都设置为 false 时
     * 预期结果：任务收集器应返回空列表
     * <p>
     * 说明：此测试需要 mock 对象 mockClass、mockMethod 和 mockField 的配合使用
     */
    @Test
    @DisplayName("测试所有配置都禁用时不生成任务")
    void testCollectFromElement_allDisabled() {
        realSettings.generateForClass = false;
        realSettings.generateForMethod = false;
        realSettings.generateForField = false;

        when(mockClass.getMethods()).thenReturn(new PsiMethod[] {mockMethod});
        when(mockClass.getFields()).thenReturn(new PsiField[] {mockField});

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockClass);

        assertThat(tasks).isEmpty();
    }

    /**
     * 测试从元素中收集文档任务功能
     * <p>
     * 测试场景：当测试方法同时包含 JUnit4 和 JUnit5 的测试注解时
     * 预期结果：应正确识别为测试方法并生成一个文档任务
     * <p>
     * 特殊说明：该测试需要模拟注解查找和任务收集过程，验证注解识别逻辑是否正确
     */
    @Test
    @DisplayName("测试识别测试方法 - 两种注解都存在")
    void testCollectFromElement_testMethod_bothAnnotations() {
        when(mockModifierList.findAnnotation("org.junit.Test")).thenReturn(mockJUnit4Annotation);
        when(mockModifierList.findAnnotation("org.junit.jupiter.api.Test")).thenReturn(mockJUnit5Annotation);

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockTestMethod);

        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        // 只要有一个测试注解就识别为测试方法
        assertThat(task.getType()).isEqualTo(DocumentationTask.TaskType.TEST_METHOD);
    }

    /**
     * 测试 collectFromElement 方法在普通方法上是否能正确识别并收集任务信息
     * <p>
     * 测试场景：被测试方法未标注任何测试注解（如 @Test 或 @org.junit.jupiter.api.Test）
     * 预期结果：应收集到一个类型为 METHOD 的 DocumentationTask 任务
     * <p>
     * 说明：该测试模拟了 mockModifierList 返回 null 的情况，验证方法未被识别为测试方法时的处理逻辑
     */
    @Test
    @DisplayName("测试普通方法不被识别为测试方法")
    void testCollectFromElement_normalMethod_notTestMethod() {
        when(mockModifierList.findAnnotation("org.junit.Test")).thenReturn(null);
        when(mockModifierList.findAnnotation("org.junit.jupiter.api.Test")).thenReturn(null);

        List<DocumentationTask> tasks = taskCollector.collectFromElement(mockMethod);

        assertThat(tasks).hasSize(1);
        DocumentationTask task = tasks.get(0);
        assertThat(task.getType()).isEqualTo(DocumentationTask.TaskType.METHOD);
    }
}

