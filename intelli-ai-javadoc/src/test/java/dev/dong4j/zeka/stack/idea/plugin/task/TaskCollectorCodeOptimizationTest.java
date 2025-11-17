package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.project.Project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskCollector 代码优化功能测试类
 * <p>
 * 该类用于测试 TaskCollector 中的代码优化功能，包括删除空行、删除单行注释、保留 JavaDoc 注释、行数截取、禁用优化以及空代码和 null 处理等场景。
 * <p>
 * 测试通过反射调用 TaskCollector 的私有方法 optimizeClassCode，验证其在不同输入情况下的行为是否符合预期。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@DisplayName("TaskCollector 代码优化功能测试")
class TaskCollectorCodeOptimizationTest {
    /** 模拟的 Project 对象，用于单元测试 */
    @Mock
    private Project mockProject;
    /** 设置状态信息 */
    private SettingsState settings;
    /** 任务收集器，用于收集和管理任务信息 */
    private TaskCollector taskCollector;

    /**
     * 初始化测试环境，设置 SettingsState 和 TaskCollector 实例
     * <p>
     * 该方法在每个测试用例执行前被调用，用于初始化必要的测试对象
     * 包括创建 SettingsState 实例和 TaskCollector 实例
     */
    @BeforeEach
    void setUp() {
        settings = new SettingsState();
        taskCollector = new TaskCollector(mockProject);
    }

    /**
     * 测试代码优化功能 - 删除空行
     * <p>
     * 测试场景：对包含多余空行的 Java 类代码进行优化处理
     * 预期结果：优化后的代码应去除所有空行，保留原有逻辑结构
     */
    @Test
    @DisplayName("测试代码优化功能 - 删除空行")
    void testOptimizeClassCode_removeEmptyLines() {
        // 使用反射访问私有方法进行测试
        String originalCode = """
            public class TestClass {
            
                private String name;
            
                public String getName() {
                    return name;
                }
            
            }
            """;

        String optimized = invokeOptimizeClassCode(originalCode);

        // 验证空行被删除
        assertThat(optimized).doesNotContain("\n\n");
        assertThat(optimized).contains("public class TestClass {");
        assertThat(optimized).contains("private String name;");
        assertThat(optimized).contains("public String getName() {");
    }

    /**
     * 测试代码优化功能 - 删除单行注释
     * <p>
     * 测试场景：验证代码优化器能够正确删除所有单行注释，同时保留代码结构和内容不变
     * 预期结果：优化后的代码应不包含任何单行注释，但应保留原始代码中的类定义、字段和方法
     * <p>
     * 注意：测试使用了一个包含多个单行注释的示例代码，用于验证删除逻辑的全面性
     */
    @Test
    @DisplayName("测试代码优化功能 - 删除单行注释")
    void testOptimizeClassCode_removeSingleLineComments() {
        String originalCode = """
            public class TestClass {
                // 这是一个测试类
                private String name;
            
                // 获取名称的方法
                public String getName() {
                    return name; // 返回名称
                }
            }
            """;

        String optimized = invokeOptimizeClassCode(originalCode);

        // 验证单行注释被删除
        assertThat(optimized).doesNotContain("// 这是一个测试类");
        assertThat(optimized).doesNotContain("// 获取名称的方法");
        assertThat(optimized).doesNotContain("// 返回名称");

        // 验证代码保留
        assertThat(optimized).contains("public class TestClass {");
        assertThat(optimized).contains("private String name;");
        assertThat(optimized).contains("public String getName() {");
    }

    /**
     * 测试代码优化功能中的 JavaDoc 注释保留逻辑
     * <p>
     * 测试场景：验证优化代码时是否能够正确保留原有的 JavaDoc 注释
     * 预期结果：优化后的代码应包含原始的 JavaDoc 注释内容，包括类注释和方法注释
     * <p>
     * 特殊说明：测试数据中包含一个带有 JavaDoc 注释的测试类，优化过程应确保注释内容完整保留
     */
    @Test
    @DisplayName("测试代码优化功能 - 保留 JavaDoc 注释")
    void testOptimizeClassCode_preserveJavaDocComments() {
        String originalCode = """
            /**
             * 测试类
             * <p>这是一个用于测试的类
             */
            public class TestClass {
                /**
                 * 获取名称
                 * @return 名称
                 */
                public String getName() {
                    return name;
                }
            }
            """;

        String optimized = invokeOptimizeClassCode(originalCode);

        // 验证 JavaDoc 注释被保留
        assertThat(optimized).contains("/**");
        assertThat(optimized).contains("测试类");
        assertThat(optimized).contains("获取名称");
        assertThat(optimized).contains("@return 名称");

        // 验证代码保留
        assertThat(optimized).contains("public class TestClass {");
        assertThat(optimized).contains("public String getName() {");
    }

    /**
     * 测试代码优化功能中的行数截取逻辑
     * <p>
     * 测试场景：当设置的最大行数小于实际代码行数时
     * 预期结果：优化后的代码应保留前几行，并在末尾添加截取提示信息
     * <p>
     * 特殊说明：测试中设置 settings.maxClassCodeLines 为 5，原代码有 6 行，预期截取后仅保留前 5 行，并提示“// ... (代码已截取，超过 5 行)”
     */
    @Test
    @DisplayName("测试代码优化功能 - 行数截取")
    void testOptimizeClassCode_lineTruncation() {
        // 设置较小的最大行数进行测试
        settings.maxClassCodeLines = 5;

        String originalCode = """
            public class TestClass {
                private String field1;
                private String field2;
                private String field3;
                private String field4;
                private String field5;
                private String field6;
            }
            """;

        String optimized = invokeOptimizeClassCode(originalCode);

        // 验证代码被截取
        assertThat(optimized).contains("// ... (代码已截取，超过 5 行)");
        assertThat(optimized).contains("public class TestClass {");
        assertThat(optimized).contains("private String field1;");

        // 验证后面的字段被截取
        assertThat(optimized).doesNotContain("private String field6;");
    }

    /**
     * 测试代码优化功能 - 禁用优化场景
     * <p>
     * 测试目标：验证当禁用代码优化时，优化方法不会对原始代码进行任何修改
     * 测试场景：设置 enableCodeCompression 为 false，传入包含注释的原始代码
     * 预期结果：优化后的代码应与原始代码完全一致
     * <p>
     * 注意：测试中使用了 {@link #invokeOptimizeClassCode(String)} 方法进行代码优化操作
     */
    @Test
    @DisplayName("测试代码优化功能 - 禁用优化")
    void testOptimizeClassCode_disabled() {
        settings.enableCodeCompression = false;

        String originalCode = """
            public class TestClass {
            
                // 这是一个注释
                private String name;
            
            }
            """;

        String optimized = invokeOptimizeClassCode(originalCode);

        // 验证代码没有被优化（应该返回原始代码）
        assertThat(optimized).isEqualTo(originalCode);
    }

    /**
     * 测试代码优化功能 - 空代码处理
     * <p>
     * 测试场景：输入为空字符串时
     * 预期结果：应返回原始空字符串
     * <p>
     * 该测试验证当传入空代码时，优化方法能够正确返回原始值，确保空值处理逻辑正常
     */
    @Test
    @DisplayName("测试代码优化功能 - 空代码处理")
    void testOptimizeClassCode_emptyCode() {
        String originalCode = "";
        String optimized = invokeOptimizeClassCode(originalCode);

        // 验证空代码返回原始值
        assertThat(optimized).isEqualTo(originalCode);
    }

    /**
     * 测试代码优化功能中的 null 处理逻辑
     * <p>
     * 测试场景：输入代码为 null 时
     * 预期结果：应返回原始值，确保 null 输入不会导致异常或错误处理
     */
    @Test
    @DisplayName("测试代码优化功能 - null 处理")
    void testOptimizeClassCode_nullCode() {
        String originalCode = null;
        String optimized = invokeOptimizeClassCode(originalCode);

        // 验证 null 代码返回原始值
        assertThat(optimized).isEqualTo(originalCode);
    }

    /**
     * 通过反射调用 TaskCollector 类的私有方法 optimizeClassCode
     * <p>
     * 该方法使用 Java 反射机制调用 TaskCollector 类中定义的私有方法 optimizeClassCode，
     * 并传入原始代码字符串作为参数，返回处理后的代码字符串。
     *
     * @param originalCode 原始代码字符串
     * @return 处理后的代码字符串
     * @throws RuntimeException 如果调用过程中发生异常
     */
    private String invokeOptimizeClassCode(String originalCode) {
        try {
            java.lang.reflect.Method method = TaskCollector.class.getDeclaredMethod("optimizeClassCode", String.class);
            method.setAccessible(true);
            return (String) method.invoke(taskCollector, originalCode);
        } catch (Exception e) {
            throw new RuntimeException("无法调用 optimizeClassCode 方法", e);
        }
    }
}
