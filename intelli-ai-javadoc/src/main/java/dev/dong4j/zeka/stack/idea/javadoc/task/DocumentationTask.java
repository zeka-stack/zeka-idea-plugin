package dev.dong4j.zeka.stack.idea.javadoc.task;

import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * 文档生成任务类
 * <p>
 * 用于封装代码文档生成任务的相关信息, 包括待处理的代码元素, 代码内容, 任务类型, 文件路径等.
 * 该类支持不同类型的任务 (类, 方法, 测试方法, 字段, 接口, 枚举), 并跟踪任务的执行状态.
 * 提供了任务状态管理, 结果存储和错误信息记录等功能, 是文档自动生成系统的核心任务实体.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class DocumentationTask {

    /**
     * 当前 Psi 元素对象, 用于表示代码中的语法结构节点
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
     */
    private final PsiElement element;
    /** 业务操作的唯一标识码 */
    private final String code;
    /** 任务类型, 表示当前任务的类型信息 */
    private final TaskType type;
    /**
     * 文件路径
     * <p>
     * 表示当前任务所关联的代码文件在系统中的存储路径.
     */
    private final String filePath;
    /**
     * 任务上下文信息
     * <p>
     * 包含与当前元素相关的额外上下文, 如所属类的代码片段等, 用于帮助 AI 生成更精准的注释.
     * 该字段为可选, 某些场景下可能不存在上下文信息.
     */
    @NotNull
    @Getter
    private final GenerationContext context;
    /**
     * 任务当前状态
     * <p>
     * 表示当前任务的执行状态, 可取值为 PENDING,PROCESSING,COMPLETED,FAILED 或 SKIPPED.
     */
    private TaskStatus status;
    /**
     * 结果数据
     * <p>
     * 用于存储任务执行的结果信息, 例如生成的文档内容或其他处理结果.
     * 该字段为可空, 根据任务执行情况可能包含有效数据或保持为空.
     *
     */
    @Setter
    @Getter
    private String result;
    /**
     * 错误信息描述
     * <p>
     * 用于存储和获取任务执行过程中产生的错误信息, 便于后续的调试和日志记录.
     */
    @Getter
    @Setter
    private String errorMessage;

    /**
     * 任务类型枚举
     * <p>
     * 定义了系统中不同类型的任务类型, 用于区分和标识各种任务的执行目标,
     * 包括类任务, 方法任务, 测试方法任务, 字段任务, 接口任务和枚举任务
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.30
     * @since 1.0.0
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
     * 任务状态枚举
     * <p>
     * 定义任务执行过程中的各种状态, 包括待处理, 处理中, 已完成, 失败和跳过等状态
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
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
     * @param context  任务上下文信息（可为 null）
     */
    public DocumentationTask(@NotNull PsiElement element,
                             @NotNull String code,
                             @NotNull TaskType type,
                             @NotNull String filePath,
                             @NotNull GenerationContext context) {
        this.element = element;
        this.code = code;
        this.type = type;
        this.filePath = filePath;
        this.context = context;
        this.status = TaskStatus.PENDING;
    }

    /**
     * 构造一个不携带上下文信息的 DocumentationTask 对象
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
        this(element, code, type, filePath, GenerationContext.empty());
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

