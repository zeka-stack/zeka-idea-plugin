package dev.dong4j.zeka.stack.idea.javadoc.task;

import com.intellij.psi.PsiElement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * DocumentationTask 单元测试类
 * <p>
 * 该类用于对 DocumentationTask 类进行单元测试，验证其各个方法和属性的正确性，包括任务状态的变更、结果的设置、错误信息的设置、任务类型的识别以及元素名称的生成等功能。
 * <p>
 * 测试覆盖了任务生命周期的各个状态（待处理、处理中、完成、失败、跳过），以及不同任务类型（类、方法、测试方法、字段、接口、枚举）的处理逻辑。
 *
 * @author 作者信息
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@DisplayName("DocumentationTask 单元测试")
public class DocumentationTaskTest {

    /** 模拟的 PsiElement 对象，用于单元测试中模拟代码元素行为 */
    @Mock
    private PsiElement mockElement;

    /** 当前文档任务对象 */
    private DocumentationTask task;
    /** 测试代码示例，用于演示方法定义格式 */
    private static final String TEST_CODE = "public void testMethod() { return 42; }";
    /** 测试文件路径，用于指定测试用例文件的存储位置 */
    private static final String TEST_FILE_PATH = "/path/to/TestFile.java";

    /**
     * 初始化测试环境，设置 mock 对象和任务实例
     * <p>
     * 该方法在每个测试用例执行前调用，用于初始化 mock 元素和创建 DocumentationTask 实例
     * 设置 mockElement 的 getText 方法返回 TEST_CODE 常量，并初始化 task 变量
     *
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockElement.getText()).thenReturn(TEST_CODE);

        task = new DocumentationTask(
            mockElement,
            TEST_CODE,
            DocumentationTask.TaskType.METHOD,
            TEST_FILE_PATH
        );
    }

    /**
     * 测试任务创建功能
     * <p>
     * 测试场景：验证新任务对象创建后各属性值是否正确初始化
     * 预期结果：任务对象的各个属性应与预期值一致，包括元素、代码、类型、文件路径、状态、结果和错误信息
     * <p>
     * 注意：测试中使用了 mock 对象 mockElement，需确保其正确配置
     */
    @Test
    @DisplayName("测试任务创建")
    void testTaskCreation() {
        assertThat(task.getElement()).isEqualTo(mockElement);
        assertThat(task.getCode()).isEqualTo(TEST_CODE);
        assertThat(task.getType()).isEqualTo(DocumentationTask.TaskType.METHOD);
        assertThat(task.getFilePath()).isEqualTo(TEST_FILE_PATH);
        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.PENDING);
        assertThat(task.getResult()).isNull();
        assertThat(task.getErrorMessage()).isNull();
    }

    /**
     * 测试任务状态变更功能
     * <p>
     * 测试场景：验证任务状态在不同变更操作下的正确性
     * 预期结果：每次调用 setStatus 方法后，任务状态应更新为指定值
     * <p>
     * 测试步骤：
     * 1. 初始状态应为 PENDING
     * 2. 将状态设置为 PROCESSING，验证状态是否更新
     * 3. 将状态设置为 COMPLETED，验证状态是否更新
     */
    @Test
    @DisplayName("测试任务状态变更")
    void testStatusChange() {
        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.PENDING);

        task.setStatus(DocumentationTask.TaskStatus.PROCESSING);
        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.PROCESSING);

        task.setStatus(DocumentationTask.TaskStatus.COMPLETED);
        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.COMPLETED);
    }

    /**
     * 测试任务状态设置为失败的情况
     * <p>
     * 测试场景：当任务状态被标记为失败，并设置相应的错误信息
     * 预期结果：验证任务状态是否正确设置为 FAILED，且错误信息是否匹配
     * <p>
     * 特殊说明：需确保任务对象初始化正确，且状态和错误信息的设置逻辑正常
     */
    @Test
    @DisplayName("测试任务状态 - 失败")
    void testStatusFailed() {
        task.setStatus(DocumentationTask.TaskStatus.FAILED);
        task.setErrorMessage("API 调用失败");

        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo("API 调用失败");
    }

    /**
     * 测试任务状态设置为“跳过”时的正确性
     * <p>
     * 测试场景：将任务状态设置为 SKIPPED
     * 预期结果：验证任务状态能够正确设置为 SKIPPED
     */
    @Test
    @DisplayName("测试任务状态 - 跳过")
    void testStatusSkipped() {
        task.setStatus(DocumentationTask.TaskStatus.SKIPPED);

        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.SKIPPED);
    }

    /**
     * 测试设置任务结果功能
     * <p>
     * 测试场景：验证设置任务结果的方法是否正常工作
     * 预期结果：任务结果应被正确设置并返回
     */
    @Test
    @DisplayName("测试设置结果")
    void testSetResult() {
        String expectedResult = "/** 测试方法 Javadoc */";
        task.setResult(expectedResult);

        assertThat(task.getResult()).isEqualTo(expectedResult);
    }

    /**
     * 测试设置错误消息功能
     * <p>
     * 测试场景：验证当调用 setErrorMessage 方法设置错误信息后，能否正确获取到该信息
     * 预期结果：任务对象的错误消息应与设置的值一致
     */
    @Test
    @DisplayName("测试设置错误消息")
    void testSetErrorMessage() {
        String errorMessage = "网络连接失败";
        task.setErrorMessage(errorMessage);

        assertThat(task.getErrorMessage()).isEqualTo(errorMessage);
    }

    /**
     * 测试任务类型为 CLASS 的功能
     * <p>
     * 测试场景：创建一个类型为 CLASS 的 DocumentationTask 实例
     * 预期结果：任务类型应正确识别为 CLASS
     * <p>
     * 该测试用于验证 DocumentationTask 构造方法是否能正确设置任务类型
     */
    @Test
    @DisplayName("测试任务类型 - CLASS")
    void testTaskTypeClass() {
        DocumentationTask classTask = new DocumentationTask(
            mockElement,
            "public class TestClass {}",
            DocumentationTask.TaskType.CLASS,
            TEST_FILE_PATH
        );

        assertThat(classTask.getType()).isEqualTo(DocumentationTask.TaskType.CLASS);
    }

    /**
     * 测试任务类型获取功能
     * <p>
     * 测试场景：验证任务类型是否为 METHOD
     * 预期结果：应返回 DocumentationTask.TaskType.METHOD
     */
    @Test
    @DisplayName("测试任务类型 - METHOD")
    void testTaskTypeMethod() {
        assertThat(task.getType()).isEqualTo(DocumentationTask.TaskType.METHOD);
    }

    /**
     * 测试 DocumentationTask 类中任务类型为 TEST_METHOD 的情况
     * <p>
     * 测试场景：创建一个 DocumentationTask 实例，其任务类型为 TEST_METHOD
     * 预期结果：验证任务类型是否正确设置为 TEST_METHOD
     * <p>
     * 该测试用于确保任务类型字段在实例化时被正确赋值
     */
    @Test
    @DisplayName("测试任务类型 - TEST_METHOD")
    void testTaskTypeTestMethod() {
        DocumentationTask testMethodTask = new DocumentationTask(
            mockElement,
            "@Test public void testSomething() {}",
            DocumentationTask.TaskType.TEST_METHOD,
            TEST_FILE_PATH
        );

        assertThat(testMethodTask.getType()).isEqualTo(DocumentationTask.TaskType.TEST_METHOD);
    }

    /**
     * 测试任务类型为 FIELD 的功能
     * <p>
     * 测试场景：创建一个任务类型为 FIELD 的 DocumentationTask 实例
     * 预期结果：任务类型应正确识别为 FIELD
     * <p>
     * 该测试用于验证 DocumentationTask 构造方法是否能正确识别并设置任务类型为 FIELD
     */
    @Test
    @DisplayName("测试任务类型 - FIELD")
    void testTaskTypeField() {
        DocumentationTask fieldTask = new DocumentationTask(
            mockElement,
            "private String username;",
            DocumentationTask.TaskType.FIELD,
            TEST_FILE_PATH
        );

        assertThat(fieldTask.getType()).isEqualTo(DocumentationTask.TaskType.FIELD);
    }

    /**
     * 测试任务类型为 INTERFACE 的功能
     * <p>
     * 测试场景：创建一个类型为 INTERFACE 的 DocumentationTask 实例
     * 预期结果：任务类型应正确设置为 INTERFACE
     * <p>
     * 该测试用于验证 DocumentationTask 构造函数是否能正确识别并设置任务类型为 INTERFACE
     */
    @Test
    @DisplayName("测试任务类型 - INTERFACE")
    void testTaskTypeInterface() {
        DocumentationTask interfaceTask = new DocumentationTask(
            mockElement,
            "public interface UserService {}",
            DocumentationTask.TaskType.INTERFACE,
            TEST_FILE_PATH
        );

        assertThat(interfaceTask.getType()).isEqualTo(DocumentationTask.TaskType.INTERFACE);
    }

    /**
     * 测试任务类型枚举功能
     * <p>
     * 测试场景：验证 DocumentationTask 类在处理枚举类型时任务类型的正确性
     * 预期结果：应返回 DocumentationTask.TaskType.ENUM 类型
     * <p>
     * 该测试用于确保枚举类型的识别逻辑正常工作，适用于代码文档生成场景
     */
    @Test
    @DisplayName("测试任务类型 - ENUM")
    void testTaskTypeEnum() {
        DocumentationTask enumTask = new DocumentationTask(
            mockElement,
            "public enum Status { ACTIVE, INACTIVE }",
            DocumentationTask.TaskType.ENUM,
            TEST_FILE_PATH
        );

        assertThat(enumTask.getType()).isEqualTo(DocumentationTask.TaskType.ENUM);
    }

    /**
     * 测试获取元素显示名称功能 - 短代码
     * <p>
     * 测试场景：当元素类型为 METHOD 且代码为短代码时
     * 预期结果：应返回 "short code..." 作为元素名称
     * <p>
     * 注意：该测试依赖 mockElement 对象和 TEST_FILE_PATH 路径的正确配置
     */
    @Test
    @DisplayName("测试获取元素显示名称 - 短代码")
    void testGetElementName_shortCode() {
        DocumentationTask shortTask = new DocumentationTask(
            mockElement,
            "short code",
            DocumentationTask.TaskType.METHOD,
            TEST_FILE_PATH
        );

        String elementName = shortTask.getElementName();
        assertThat(elementName).isEqualTo("short code...");
    }

    /**
     * 测试获取元素显示名称功能
     * <p>
     * 测试场景：当元素的代码内容较长时，需验证生成的名称是否正确截断并添加省略号
     * 预期结果：生成的名称长度应为53（50字符+ "...")，以"public void very"开头，并以"..."结尾
     * <p>
     * 注意：测试中使用了模拟对象mockElement，其getText()方法返回一个很长的方法名字符串
     */
    @Test
    @DisplayName("测试获取元素显示名称 - 长代码")
    void testGetElementName_longCode() {
        String longCode = "public void veryLongMethodNameThatExceedsFiftyCharactersForTesting() { return; }";
        when(mockElement.getText()).thenReturn(longCode);

        DocumentationTask longTask = new DocumentationTask(
            mockElement,
            longCode,
            DocumentationTask.TaskType.METHOD,
            TEST_FILE_PATH
        );

        String elementName = longTask.getElementName();
        assertThat(elementName).hasSize(53); // 50 + "..."
        assertThat(elementName).endsWith("...");
        assertThat(elementName).startsWith("public void very");
    }

    /**
     * 测试 toString 方法
     * <p>
     * 测试场景：验证 Task 对象的 toString 方法是否正确生成包含关键信息的字符串表示
     * 预期结果：生成的字符串应包含 "DocumentationTask"、"type=METHOD"、"filePath='/path/to/TestFile.java'" 和 "status=PENDING" 等关键信息
     */
    @Test
    @DisplayName("测试 toString 方法")
    void testToString() {
        String result = task.toString();

        assertThat(result).contains("DocumentationTask");
        assertThat(result).contains("type=METHOD");
        assertThat(result).contains("filePath='/path/to/TestFile.java'");
        assertThat(result).contains("status=PENDING");
    }

    /**
     * 测试完整的任务生命周期
     * <p>
     * 测试场景：模拟任务从初始状态（PENDING）到处理中（PROCESSING）再到完成状态（COMPLETED）的整个流程
     * 预期结果：任务状态应正确更新，结果应设置为指定的文档内容，错误信息应保持为空
     * <p>
     * 特殊说明：需要确保任务对象的各个状态转换逻辑正确，并且相关字段的赋值与获取方法正常工作
     */
    @Test
    @DisplayName("测试完整的任务生命周期")
    void testCompleteTaskLifecycle() {
        // 1. 初始状态
        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.PENDING);
        assertThat(task.getResult()).isNull();
        assertThat(task.getErrorMessage()).isNull();

        // 2. 开始处理
        task.setStatus(DocumentationTask.TaskStatus.PROCESSING);
        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.PROCESSING);

        // 3. 完成处理
        String javadoc = "/** 测试方法文档 */";
        task.setResult(javadoc);
        task.setStatus(DocumentationTask.TaskStatus.COMPLETED);

        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.COMPLETED);
        assertThat(task.getResult()).isEqualTo(javadoc);
        assertThat(task.getErrorMessage()).isNull();
    }

    /**
     * 测试任务生命周期在处理失败时的行为
     * <p>
     * 测试场景：任务从初始状态开始，经过处理阶段后因API请求超时而失败
     * 预期结果：任务状态应更新为失败，错误信息应设置为"API 请求超时"，结果应为null
     * <p>
     * 该测试验证任务状态转换及错误信息记录的正确性
     */
    @Test
    @DisplayName("测试失败的任务生命周期")
    void testFailedTaskLifecycle() {
        // 1. 初始状态
        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.PENDING);

        // 2. 开始处理
        task.setStatus(DocumentationTask.TaskStatus.PROCESSING);

        // 3. 处理失败
        String errorMsg = "API 请求超时";
        task.setErrorMessage(errorMsg);
        task.setStatus(DocumentationTask.TaskStatus.FAILED);

        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo(errorMsg);
        assertThat(task.getResult()).isNull();
    }

    /**
     * 测试任务状态为跳过的场景
     * <p>
     * 测试场景：设置任务状态为 SKIPPED，验证状态是否正确且结果和错误信息为 null
     * 预期结果：任务状态应为 SKIPPED，结果和错误信息应为 null
     */
    @Test
    @DisplayName("测试跳过的任务")
    void testSkippedTask() {
        task.setStatus(DocumentationTask.TaskStatus.SKIPPED);

        assertThat(task.getStatus()).isEqualTo(DocumentationTask.TaskStatus.SKIPPED);
        assertThat(task.getResult()).isNull();
        assertThat(task.getErrorMessage()).isNull();
    }

    /**
     * 测试任务状态枚举的所有值
     * <p>
     * 测试场景：验证任务状态枚举类包含所有定义的枚举值
     * 预期结果：枚举值应包含 PENDING、PROCESSING、COMPLETED、FAILED 和 SKIPPED
     */
    @Test
    @DisplayName("测试任务状态枚举的所有值")
    void testAllTaskStatusValues() {
        DocumentationTask.TaskStatus[] statuses = DocumentationTask.TaskStatus.values();

        assertThat(statuses).contains(
            DocumentationTask.TaskStatus.PENDING,
            DocumentationTask.TaskStatus.PROCESSING,
            DocumentationTask.TaskStatus.COMPLETED,
            DocumentationTask.TaskStatus.FAILED,
            DocumentationTask.TaskStatus.SKIPPED
                                     );
    }

    /**
     * 测试任务类型枚举的所有值是否包含预定义的类型
     * <p>
     * 测试场景：验证枚举类型 DocumentationTask.TaskType 是否包含所有预期的任务类型
     * 预期结果：枚举数组应包含 CLASS, METHOD, TEST_METHOD, FIELD, INTERFACE, ENUM 六种类型
     */
    @Test
    @DisplayName("测试任务类型枚举的所有值")
    void testAllTaskTypeValues() {
        DocumentationTask.TaskType[] types = DocumentationTask.TaskType.values();

        assertThat(types).contains(
            DocumentationTask.TaskType.CLASS,
            DocumentationTask.TaskType.METHOD,
            DocumentationTask.TaskType.TEST_METHOD,
            DocumentationTask.TaskType.FIELD,
            DocumentationTask.TaskType.INTERFACE,
            DocumentationTask.TaskType.ENUM
                                  );
    }
}

