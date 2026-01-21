package dev.dong4j.zeka.stack.idea.plugin.terminal.context;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.frontend.view.TerminalView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TerminalContextService
 * <p> 该服务主要用于在 IntelliJ IDEA 的终端窗口中获取和缓存上下文信息, 以便为 AI 对话生成更精准的提示. 它会根据当前工作目录检测项目结构, 技术栈以及历史对话, 并将这些信息组合成用户需求提示.</p>
 * <p> 核心功能包括:</p>
 * <pre>{@code
 * 1. 根据当前目录构建 AI 提示 (buildUserPrompt)
 * 2. 收集 TerminalView 上下文信息 (collectContext)
 * 3. 记录并维护最近的 AI 对话历史
 * 4. 缓存 projectInfo 与 techStack 以提升性能
 * 5. 资源释放 (dispose)
 * }</pre>
 * <p> 使用场景: 与插件提供的 AI 辅助工具结合, 实时为开发者提供基于当前项目与环境的智能建议.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.21
 * @since x.x.x
 */
@SuppressWarnings("UnstableApiUsage")
@Service(Service.Level.PROJECT)
public final class TerminalContextService implements com.intellij.openapi.Disposable {

    /** 当前项目实例, 用于访问项目相关资源和配置信息 */
    private final Project project;
    /** 用于缓存终端上下文信息的映射表, 以当前目录路径为键, 存储对应的上下文数据, 避免重复检测项目信息 */
    private final Map<String, CachedContext> cache = new ConcurrentHashMap<>();
    /** Shell 环境信息, 用于记录当前终端的 shell 类型和操作系统信息 */
    private final ShellEnv shellEnv;
    /** AI 交互历史记录队列, 用于存储用户与 AI 之间的对话历史, 以便在生成提示词时提供上下文信息 */
    private final Deque<AiHistoryEntry> history = new ArrayDeque<>();
    /**
     * 最大历史记录条数
     * <p> 定义了 AI 生成历史记录的最大缓存数量, 超出此数量的历史记录将被移除.</p>
     *
     * @see #history
     */
    private static final int MAX_HISTORY = 5;

    /**
     * 初始化终端上下文服务
     * <p> 根据传入的项目实例初始化终端上下文服务, 并检测当前系统的 shell 环境信息.</p>
     *
     * @param project 项目实例, 用于获取基础路径和管理资源
     * @since 1.0.0
     */
    public TerminalContextService(@NotNull Project project) {
        this.project = project;
        this.shellEnv = detectShellEnv();
    }

    /**
     * 构建用户的增强提示词
     * <p> 根据用户输入和终端视图, 调用 {@link #collectContext(TerminalView)} 收集当前目录的上下文信息, 然后通过 {@link #buildPrompt(ContextInfo, String)}
     * 方法拼接成结构化的提示词字符串, 用于增强 AI 交互或终端辅助功能.</p>
     *
     * @param userInput    用户输入的原始请求内容, 不能为空
     * @param terminalView 终端视图对象, 可以为空, 用于获取当前工作目录
     * @return 构建完成的增强提示词字符串, 包含上下文信息和用户需求
     */
    @NotNull
    public String buildUserPrompt(@NotNull String userInput, @Nullable TerminalView terminalView) {
        ContextInfo context = collectContext(terminalView);
        return buildPrompt(context, userInput);
    }

    /**
     * 收集与终端当前目录相关的上下文信息
     * <p> 根据给定的终端视图获取当前目录, 并构建包含操作系统环境, 项目信息和技术栈的上下文信息.
     * 如果当前目录已缓存, 则直接从缓存中获取; 否则, 检测项目信息和技术栈, 并将其缓存.</p>
     *
     * @param terminalView 终端视图, 可以为 null
     * @return 包含操作系统环境, 项目信息和技术栈的上下文信息
     */
    @NotNull
    public ContextInfo collectContext(@Nullable TerminalView terminalView) {
        String currentDirectory = terminalView != null ? terminalView.getCurrentDirectory() : null;
        VirtualFile currentDir = resolveDirectory(currentDirectory);
        VirtualFile projectRoot = resolveProjectRoot(project.getBasePath());
        if (currentDir == null) {
            return new ContextInfo(shellEnv, ProjectInfo.empty(), TechStack.empty(), currentDirectory, historySnapshot());
        }
        String cacheKey = currentDir.getPath();
        CachedContext cached = cache.get(cacheKey);
        if (cached != null) {
            return new ContextInfo(shellEnv, cached.projectInfo, cached.techStack, currentDirectory, historySnapshot());
        }
        ProjectInfo projectInfo = detectProjectInfo(currentDir, projectRoot);
        TechStack techStack = detectTechStack(currentDir);
        CachedContext context = new CachedContext(projectInfo, techStack);
        cache.put(cacheKey, context);
        return new ContextInfo(shellEnv, projectInfo, techStack, currentDirectory, historySnapshot());
    }

    /**
     * 释放资源
     * <p> 清理缓存和历史记录, 释放占用的资源. 当服务被销毁时由框架调用 </p>
     */
    @Override
    public void dispose() {
        cache.clear();
        history.clear();
    }

    /**
     * 根据上下文信息和用户输入构建增强的提示词字符串
     * <p>该方法将当前目录的环境信息 (如操作系统,Shell,Git 状态, 技术栈等) 与用户输入
     * 拼接成结构化提示词, 用于增强 AI 交互或终端辅助功能.</p>
     * <p>输出格式示例:</p>
     * <pre>{@code
     * 当前目录上下文:
     * -os: linux
     * -shell: bash
     * -current-directory:/home/user/project
     * -git: true
     * -dockerfile: true
     * -docker-compose: true
     * -kubernetes: false
     * -build-tool: maven
     * -language: java
     *
     * 用户需求:
     * 请帮我生成一个 Spring Boot 的启动类
     * }</pre>
     *
     * @param context   上下文信息对象, 包含 Shell 环境, 项目信息和技术栈
     * @param userInput 用户输入的原始请求内容
     * @return 构建完成的提示词字符串, 包含上下文信息和用户需求
     */
    @NotNull
    private static String buildPrompt(@NotNull ContextInfo context, @NotNull String userInput) {
        StringBuilder sb = new StringBuilder();
        sb.append("【当前目录上下文】:\n");
        sb.append("- os: ").append(context.shellEnv.os).append("\n");
        sb.append("- shell: ").append(context.shellEnv.shell).append("\n");
        if (!StringUtil.isEmptyOrSpaces(context.currentDirectory)) {
            sb.append("- current-directory: ").append(context.currentDirectory).append("\n");
        }
        sb.append("- git: ").append(context.projectInfo.isGit).append("\n");
        if (!StringUtil.isEmptyOrSpaces(context.projectInfo.gitConfig)) {
            sb.append("- git-config:\n");
            sb.append(context.projectInfo.gitConfig).append("\n");
        }
        if (context.projectInfo.hasDockerfile) {
            sb.append("- dockerfile: true\n");
        }
        if (context.projectInfo.hasDockerCompose) {
            sb.append("- docker-compose: true\n");
        }
        if (context.projectInfo.hasKubernetes) {
            sb.append("- kubernetes: true\n");
        }
        if (context.techStack.buildTool != null) {
            sb.append("- build-tool: ").append(context.techStack.buildTool).append("\n");
        }
        if (context.techStack.language != null) {
            sb.append("- language: ").append(context.techStack.language).append("\n");
        }
        if (!context.history.isEmpty()) {
            sb.append("- ai-history:\n");
            for (AiHistoryEntry entry : context.history) {
                sb.append("  - Q: ").append(entry.question).append("\n");
                sb.append("    A: ").append(entry.answer).append("\n");
            }
        }
        sb.append("\n【用户需求】:\n");
        sb.append(userInput);
        return sb.toString();
    }

    /**
     * 解析目录路径为 VirtualFile 对象
     * <p> 尝试根据当前目录路径查找对应的 VirtualFile 对象, 如果目录路径为空或未找到对应目录则返回 null.
     * 该方法使用 LocalFileSystem 来解析文件系统路径.
     *
     * @param currentDirectory 当前目录路径, 可以为 null
     * @return 解析后的 VirtualFile 对象, 如果目录不存在或路径为空则返回 null
     */
    @Nullable
    private static VirtualFile resolveDirectory(@Nullable String currentDirectory) {
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        if (!StringUtil.isEmptyOrSpaces(currentDirectory)) {
            return lfs.findFileByPath(currentDirectory);
        }
        return null;
    }

    /**
     * 根据项目基础路径解析项目根目录的 VirtualFile 对象
     * <p> 如果项目基础路径不为空或空格, 则尝试将其转换为对应的 VirtualFile 对象;
     * 如果路径为空或无效, 则直接返回 null
     *
     * @param projectBasePath 项目基础路径, 可以为 null
     * @return 对应的 VirtualFile 对象, 如果路径无效则返回 null
     */
    @Nullable
    private static VirtualFile resolveProjectRoot(@Nullable String projectBasePath) {
        if (StringUtil.isEmptyOrSpaces(projectBasePath)) {
            return null;
        }
        return LocalFileSystem.getInstance().findFileByPath(projectBasePath);
    }

    /**
     * 检测当前系统的 Shell 环境信息
     * <p> 通过读取系统环境变量和属性来获取当前 Shell 的类型和操作系统名称, 若未设置 {@code SHELL} 环境变量则使用 {@code java.io} 系统属性或默认值 {@code "bash"}.</p>
     * <p> 该方法返回 {@link ShellEnv} 对象, 包含已提取的 Shell 名称和当前操作系统.</p>
     *
     * @return ShellEnv 包含 shell 名称和操作系统名称的对象, 永不为 {@code null}
     */
    @NotNull
    private static ShellEnv detectShellEnv() {
        String shell = System.getenv("SHELL");
        if (StringUtil.isEmptyOrSpaces(shell)) {
            shell = System.getProperty("SHELL", "bash");
        }
        String osName = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
        return new ShellEnv(extractShellName(shell), osName);
    }

    /**
     * 从给定的 shell 路径中提取简化的 Shell 名称.
     * <p> 将传入的 shell 路径字符串转换为小写后, 检查是否包含关键字
     * "zsh","bash" 或 "fish". 若匹配则返回对应的 Shell 名称,
     * 否则默认返回 "bash".</p>
     *
     * @param shellPath 原始 shell 路径字符串, 不能为空
     * @return 简化后的 Shell 名称, 可能为 "zsh","bash" 或 "fish"
     */
    @NotNull
    private static String extractShellName(@NotNull String shellPath) {
        String lower = shellPath.toLowerCase(Locale.ROOT);
        if (lower.contains("zsh")) {
            return "zsh";
        }
        if (lower.contains("bash")) {
            return "bash";
        }
        if (lower.contains("fish")) {
            return "fish";
        }
        return "bash";
    }

    /**
     * 检测项目的基础设施和配置信息
     * <p> 通过扫描指定目录, 判断项目是否包含 Git 版本控制,Docker 容器化文件,
     * Docker Compose 编排配置以及 Kubernetes 部署清单.
     *
     * @param currentDir  当前扫描的虚拟文件目录
     * @param projectRoot 项目根目录, 用于限制 Git 根目录向上查找的范围
     * @return 包含检测结果的 {@link ProjectInfo} 对象, 包含 Git 支持状态和容器化配置状态
     */
    @NotNull
    private static ProjectInfo detectProjectInfo(@NotNull VirtualFile currentDir, @Nullable VirtualFile projectRoot) {
        VirtualFile gitRoot = findGitRoot(currentDir, projectRoot);
        boolean isGit = gitRoot != null;
        boolean hasDockerfile = currentDir.findChild("Dockerfile") != null;
        boolean hasDockerCompose = currentDir.findChild("docker-compose.yml") != null
                                   || currentDir.findChild("docker-compose.yaml") != null;
        boolean hasKubernetes = currentDir.findChild("k8s") != null
                                || currentDir.findChild("manifests") != null
                                || currentDir.findChild(".kube") != null
                                || currentDir.findChild(".helm") != null;
        String gitConfig = gitRoot != null ? readGitConfig(gitRoot) : null;
        return new ProjectInfo(isGit, hasDockerfile, hasDockerCompose, hasKubernetes, gitConfig);
    }

    /**
     * 根据当前目录检测项目的技术栈信息
     * <p> 通过检查目录下是否存在特定的配置文件来判断项目所使用的构建工具和编程语言.
     * 检测逻辑顺序如下:
     * <pre>{@code
     * 1. pom.xml                    -> Maven / Java
     * 2. build.gradle / .kts       -> Gradle / Java
     * 3. package.json               -> NPM / JavaScript
     * 4. go.mod                     -> Go / Go
     * 5. pyproject.toml / reqs.txt  -> Pip / Python
     * }
     * </pre>
     * 如果未匹配到任何文件, 返回对象的字段将为 null.
     *
     * @param currentDir 当前目录对应的 VirtualFile 对象, 不能为 null
     * @return 包含检测到的构建工具和编程语言的 TechStack 对象
     */
    @NotNull
    private static TechStack detectTechStack(@NotNull VirtualFile currentDir) {
        String buildTool = null;
        String language = null;
        if (currentDir.findChild("pom.xml") != null) {
            buildTool = "maven";
            language = "java";
        } else if (currentDir.findChild("build.gradle") != null
                   || currentDir.findChild("build.gradle.kts") != null) {
            buildTool = "gradle";
            language = "java";
        } else if (currentDir.findChild("package.json") != null) {
            buildTool = "npm";
            language = "javascript";
        } else if (currentDir.findChild("go.mod") != null) {
            buildTool = "go";
            language = "go";
        } else if (currentDir.findChild("pyproject.toml") != null
                   || currentDir.findChild("requirements.txt") != null) {
            buildTool = "pip";
            language = "python";
        }
        return new TechStack(buildTool, language);
    }

    /**
     * 查找 Git 根目录
     * <p> 从指定的起始目录开始向上遍历, 查找包含 ".git" 子目录的目录, 该目录即为 Git 仓库的根目录. 如果在遍历过程中遇到项目根目录, 则停止遍历.</p>
     *
     * @param start       起始目录, 用于开始查找 Git 根目录
     * @param projectRoot 项目根目录, 可为 null, 若不为 null 则在遍历到此目录时停止
     * @return 包含 ".git" 子目录的 VirtualFile 对象, 若未找到则返回 null
     */
    @Nullable
    private static VirtualFile findGitRoot(@NotNull VirtualFile start, @Nullable VirtualFile projectRoot) {
        VirtualFile current = start;
        while (current != null) {
            if (current.findChild(".git") != null) {
                return current;
            }
            if (current.equals(projectRoot)) {
                break;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 读取 Git 配置文件内容
     * <p> 从指定的 Git 根目录中查找 .git/config 文件并加载其内容. 若文件不存在或为目录, 则返回 null. 内容会被清理换行符并截断至最大 600 字符, 超出部分以省略号结尾.</p>
     *
     * @param gitRoot Git 仓库根目录的 VirtualFile 对象, 不能为空
     * @return Git 配置文件内容字符串, 若读取失败或文件不存在则返回 null
     */
    @Nullable
    private static String readGitConfig(@NotNull VirtualFile gitRoot) {
        VirtualFile gitDir = gitRoot.findChild(".git");
        if (gitDir == null) {
            return null;
        }
        VirtualFile config = gitDir.findChild("config");
        if (config == null || config.isDirectory()) {
            return null;
        }
        try {
            String content = VfsUtilCore.loadText(config);
            String normalized = content.replace("\r", "").trim();
            int max = 600;
            if (normalized.length() > max) {
                return normalized.substring(0, max).trim() + "\n...";
            }
            return normalized;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取 AI 历史记录的快照
     * <p> 创建一个新的 {@link Deque} 实例, 包含当前历史记录的所有条目, 用于避免在遍历过程中修改原始数据.</p>
     *
     * @return 包含当前 AI 历史记录的不可变队列副本
     */
    @NotNull
    private synchronized Deque<AiHistoryEntry> historySnapshot() {
        return new ArrayDeque<>(history);
    }

    /**
     * 记录 AI 交互历史
     * <p> 将用户问题和 AI 回答添加到历史记录队列中. 如果历史记录已达到最大容量, 则移除最早条目, 以保持队列长度不超过限制.</p>
     *
     * @param question 用户提出的问题, 不能为空或空白字符串
     * @param answer   AI 返回的回答, 不能为空或空白字符串
     */
    public synchronized void recordHistory(@NotNull String question, @NotNull String answer) {
        if (question.isBlank() || answer.isBlank()) {
            return;
        }
        if (history.size() >= MAX_HISTORY) {
            history.removeFirst();
        }
        history.addLast(new AiHistoryEntry(question, answer));
    }

    /**
     * 终端上下文信息记录
     * <p> 封装了终端环境, 项目状态, 技术栈以及当前目录等信息, 供 {@link TerminalContextService} 生成用户提示词时使用.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.21
     * @since 1.0.0
     */
    public record ContextInfo(@NotNull ShellEnv shellEnv,
                              @NotNull ProjectInfo projectInfo,
                              @NotNull TechStack techStack,
                              @Nullable String currentDirectory,
                              @NotNull Deque<AiHistoryEntry> history) {
    }

    /**
     * Shell 环境信息记录类
     * <p> 用于封装终端的 Shell 类型和操作系统名称.
     * 该记录主要用于存储环境检测的结果, 以便在构建上下文提示词时使用.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email mailto:dong4j@gmail.com
     * @date 2026.01.21
     * @since x.x.x
     */
    public record ShellEnv(@NotNull String shell, @NotNull String os) {
    }

    /**
     * 项目信息记录类
     * <p>该记录封装了当前项目的元数据, 包括是否为 Git 仓库, 是否包含 Dockerfile,Docker Compose 以及 Kubernetes 配置等信息.</p>
     * <p>作为不可变记录 (record) 使用, 保证线程安全, 便于缓存与比较, 常用于终端上下文服务中传递项目状态.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.21
     * @since x.x.x
     */
    public record ProjectInfo(boolean isGit,
                              boolean hasDockerfile,
                              boolean hasDockerCompose,
                              boolean hasKubernetes,
                              @Nullable String gitConfig) {
        /**
         * 创建一个空的 ProjectInfo 实例
         * <p> 该实例表示项目中不包含 Git,Dockerfile,Docker Compose 或 Kubernetes 任何相关配置 </p>
         *
         * @return 所有属性均为 false 的 ProjectInfo 对象
         */
        @NotNull
        public static ProjectInfo empty() {
            return new ProjectInfo(false, false, false, false, null);
        }
    }

    /**
     * 技术栈信息记录类
     * <p> 用于描述项目中使用的技术栈, 包括构建工具和编程语言. 该类为只读数据结构.</p>
     * <p> 提供空实例构造方法 {@code empty()}, 用于表示未检测到任何技术栈信息的默认状态.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.21
     * @since 1.0.0
     */
    public record TechStack(@Nullable String buildTool, @Nullable String language) {
        /**
         * 创建一个空的 TechStack 实例
         * <p> 该方法返回一个所有属性都为 null 的 TechStack 对象, 用于表示没有配置任何技术栈的情况.</p>
         *
         * @return 一个空的 TechStack 实例, 其中 buildTool 和 language 均为 null
         */
        @NotNull
        public static TechStack empty() {
            return new TechStack(null, null);
        }
    }

    /**
     * 缓存上下文记录类
     * <p> 用于缓存终端上下文信息, 包含项目信息和技术栈信息, 以提高性能. 该类为不可变数据结构, 适用于在终端上下文服务中存储和复用已检测的项目元数据, 避免重复扫描文件系统.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.21
     * @since 1.0.0
     */
    private record CachedContext(@NotNull ProjectInfo projectInfo, @NotNull TechStack techStack) {
    }

    /**
     * AI 历史记录条目
     * <p> 用于存储用户与 AI 之间的交互历史, 包含用户提出的问题和 AI 的回答内容.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.21
     * @since 1.0.0
     */
    private record AiHistoryEntry(@NotNull String question, @NotNull String answer) {
    }
}
