package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * 文档生成任务
 *
 * <p>表示一个需要生成文档的代码元素。
 * 作为任务执行的基本单位，封装了生成文档所需的所有信息。
 *
 * <p>任务生命周期：
 * <ol>
 *   <li>PENDING：任务创建，等待处理</li>
 *   <li>PROCESSING：任务正在处理中</li>
 *   <li>COMPLETED：任务成功完成</li>
 *   <li>FAILED：任务处理失败</li>
 *   <li>SKIPPED：任务被跳过</li>
 * </ol>
 *
 * <p>包含的信息：
 * <ul>
 *   <li>PSI 元素：需要生成文档的代码元素</li>
 *   <li>代码内容：元素的源代码（包含现有注释）</li>
 *   <li>任务类型：决定使用的 Prompt 模板</li>
 *   <li>文件路径：用于进度显示和日志记录</li>
 *   <li>处理状态：任务的当前状态</li>
 *   <li>处理结果：生成的文档内容</li>
 *   <li>错误信息：处理失败时的错误详情</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class DocumentationTask {

    /** 当前 Psi 元素对象，用于表示代码中的语法结构节点 */
    private final PsiElement element;
    /** 业务操作的唯一标识码 */
    private final String code;
    /** 任务类型，表示当前任务的类型信息 */
    private final TaskType type;
    /** 文件路径 */
    private final String filePath;
    /** 任务当前状态 */
    private TaskStatus status;
    /** 结果数据 */
    @Setter
    @Getter
    private String result;
    /** 错误信息描述 */
    @Getter
    @Setter
    private String errorMessage;

    /**
     * 任务类型
     *
     * <p>定义了支持的代码元素类型，用于区分不同的处理逻辑和 Prompt 模板。
     * 每种类型对应不同的文档生成策略。
     *
     * <p>类型说明：
     * <ul>
     *   <li>CLASS：普通类</li>
     *   <li>METHOD：普通方法</li>
     *   <li>TEST_METHOD：测试方法</li>
     *   <li>FIELD：字段（成员变量）</li>
     *   <li>INTERFACE：接口</li>
     *   <li>ENUM：枚举</li>
     * </ul>
     *
     * <p>使用场景：
     * <ul>
     *   <li>选择合适的 Prompt 模板</li>
     *   <li>决定 AI 服务的处理策略</li>
     *   <li>UI 显示和日志记录</li>
     * </ul>
     */
    public enum TaskType {
        /** 用户的唯一标识符，用于身份验证和数据关联 */
        CLASS,
        /** 方法执行结果状态码 */
        METHOD,
        /** 测试方法 */
        TEST_METHOD,
        /** 用户的唯一标识符，用于识别和区分不同用户 */
        FIELD,
        /** 接口定义 */
        INTERFACE,
        /** 枚举类型 */
        ENUM
    }

    /**
     * 任务状态
     *
     * <p>定义了任务的生命周期状态，用于跟踪任务的执行进度。
     * 状态转换遵循严格的顺序，确保任务处理的正确性。
     *
     * <p>状态说明：
     * <ul>
     *   <li>PENDING：任务已创建，等待处理</li>
     *   <li>PROCESSING：任务正在处理中</li>
     *   <li>COMPLETED：任务成功完成</li>
     *   <li>FAILED：任务处理失败</li>
     *   <li>SKIPPED：任务被跳过（根据配置）</li>
     * </ul>
     *
     * <p>状态转换：
     * <ul>
     *   <li>PENDING → PROCESSING：开始处理任务</li>
     *   <li>PROCESSING → COMPLETED：任务成功完成</li>
     *   <li>PROCESSING → FAILED：任务处理失败</li>
     *   <li>PROCESSING → SKIPPED：任务被跳过</li>
     * </ul>
     */
    public enum TaskStatus {
        /** 任务状态标识，用于表示当前任务的处理状态 */
        PENDING,    // 等待处理
        /** 用于存储用户访问令牌的值 */
        PROCESSING, // 处理中
        /** 完成状态标志 */
        COMPLETED,  // 已完成
        /** 用于标识操作失败的状态码 */
        FAILED,     // 失败
        /** 跳过标记，用于指示当前条目应被跳过处理 */
        SKIPPED     // 跳过
    }

    /**
     * 构造一个 DocumentationTask 对象
     * <p>
     * 初始化任务的基本信息，包括元素、代码内容、任务类型、文件路径和初始状态
     *
     * @param element  元素对象，表示代码中的某个元素
     * @param code     代码内容字符串
     * @param type     任务类型，表示任务的类别
     * @param filePath 文件路径，表示代码所在的文件路径
     */
    public DocumentationTask(@NotNull PsiElement element,
                             @NotNull String code,
                             @NotNull TaskType type,
                             @NotNull String filePath) {
        this.element = element;
        this.code = code;
        this.type = type;
        this.filePath = filePath;
        this.status = TaskStatus.PENDING;
    }

    /**
     * 获取当前元素对象
     * <p>
     * 返回与当前对象关联的 PsiElement 实例，该元素通常表示代码中的某个结构或节点。
     *
     * @return 当前关联的 PsiElement 对象
     */
    @NotNull
    public PsiElement getElement() {
        return element;
    }

    /**
     * 获取验证码
     * <p>
     * 返回当前存储的验证码值
     *
     * @return 验证码
     */
    @NotNull
    public String getCode() {
        return code;
    }

    /**
     * 获取任务类型
     * <p>
     * 返回当前任务的类型信息
     *
     * @return 任务类型
     */
    @NotNull
    public TaskType getType() {
        return type;
    }

    /**
     * 获取文件路径
     * <p>
     * 返回当前对象所持有的文件路径字符串。
     *
     * @return 文件路径
     */
    @NotNull
    public String getFilePath() {
        return filePath;
    }

    /**
     * 获取任务状态
     * <p>
     * 返回当前任务的执行状态
     *
     * @return 任务状态
     */
    @NotNull
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * 设置任务状态
     * <p>
     * 将任务的状态设置为指定的值
     *
     * @param status 要设置的任务状态，不能为空
     */
    public void setStatus(@NotNull TaskStatus status) {
        this.status = status;
    }

    /**
     * 获取元素的显示名称
     *
     * <p>返回元素文本的前50个字符作为显示名称，
     * 用于日志记录和调试信息显示。
     * 如果元素文本超过50个字符，会添加省略号。
     *
     * <p>设计考虑：
     * <ul>
     *   <li>限制长度避免日志过长</li>
     *   <li>提供足够的信息用于识别元素</li>
     *   <li>处理边界情况（短文本）</li>
     * </ul>
     *
     * @return 元素的显示名称（前50个字符+省略号）
     */
    @NotNull
    public String getElementName() {
        return element.getText().substring(0, Math.min(50, element.getText().length())) + "...";
    }

    /**
     * 返回该文档任务对象的字符串表示形式
     * <p>
     * 该方法重写了 Object 类的 toString 方法，用于返回一个包含任务类型、文件路径和状态的字符串信息
     *
     * @return 该文档任务对象的字符串表示
     */
    @Override
    public String toString() {
        return "DocumentationTask{" +
               "type=" + type +
               ", filePath='" + filePath + '\'' +
               ", status=" + status +
               '}';
    }
}

