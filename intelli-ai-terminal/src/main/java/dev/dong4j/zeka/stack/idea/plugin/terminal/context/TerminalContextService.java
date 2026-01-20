package dev.dong4j.zeka.stack.idea.plugin.terminal.context;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.frontend.view.TerminalView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 终端上下文服务类
 * <p> 负责在终端环境中收集项目相关的上下文信息, 包括操作系统,shell 类型, 当前目录,Git 状态,Docker/Kubernetes 文件检测以及构建工具和编程语言的识别. 这些信息用于生成用户提示内容, 以增强终端交互的智能化程度.
 *
 * @author dong4j
 * @version 1.0.0
 * @email mailto:dong4j@gmail.com
 * @date 2026.01.21
 * @since 1.0.0
 */
@SuppressWarnings("UnstableApiUsage")
@Service(Service.Level.PROJECT)
public final class TerminalContextService implements com.intellij.openapi.Disposable {

    /** 当前项目实例, 用于访问项目相关资源和配置信息 */
    private final Project project;
    /**
     * 用于缓存终端上下文信息的映射表
     * <p> 以当前目录路径为键, 存储对应的上下文数据, 避免重复检测项目信息 </p>
     *
     * @see CachedContext
     */
    private final Map<String, CachedContext> cache = new ConcurrentHashMap<>();
    /** Shell 环境信息, 用于记录当前终端的 shell 类型和操作系统信息 */
    private final ShellEnv shellEnv;

    /**
     * 初始化终端上下文服务
     * <p> 根据传入的项目实例初始化终端上下文服务, 检测当前系统的 shell 环境信息.</p>
     *
     * @param project 项目实例, 用于获取基础路径和管理资源
     */
    public TerminalContextService(@NotNull Project project) {
        this.project = project;
        this.shellEnv = detectShellEnv();
    }

    /**
     * 构建用户的提示词
     * <p> 根据给定的用户输入和终端视图, 收集当前目录的上下文信息, 并生成增强的用户提示词.</p>
     *
     * @param userInput    用户输入的内容
     * @param terminalView 终端视图对象, 可以为空
     * @return 增强的用户提示词
     */
    @NotNull
    public String buildUserPrompt(@NotNull String userInput, @Nullable TerminalView terminalView) {
        ContextInfo context = collectContext(terminalView);
        return buildPrompt(context, userInput);
    }

    /**
     * 收集与终端当前目录相关的上下文信息
     * <p> 根据给定的终端视图获取当前目录, 并构建包含操作系统环境, 项目信息和技术栈的上下文信息.
     * 如果当前目录已缓存, 则直接从缓存中获取; 否则, 检测项目信息和技术栈, 并将其缓存.
     *
     * @param terminalView 终端视图, 可以为 null
     * @return 包含操作系统环境, 项目信息和技术栈的上下文信息
     */
    @NotNull
    public ContextInfo collectContext(@Nullable TerminalView terminalView) {
        String currentDirectory = terminalView != null ? terminalView.getCurrentDirectory() : null;
        VirtualFile directory = resolveDirectory(currentDirectory, project.getBasePath());
        if (directory == null) {
            return new ContextInfo(shellEnv, ProjectInfo.empty(), TechStack.empty(), currentDirectory);
        }
        String cacheKey = directory.getPath();
        CachedContext cached = cache.get(cacheKey);
        if (cached != null) {
            return new ContextInfo(shellEnv, cached.projectInfo, cached.techStack, currentDirectory);
        }
        ProjectInfo projectInfo = detectProjectInfo(directory);
        TechStack techStack = detectTechStack(directory);
        CachedContext context = new CachedContext(projectInfo, techStack);
        cache.put(cacheKey, context);
        return new ContextInfo(shellEnv, projectInfo, techStack, currentDirectory);
    }

    /**
     * 清理缓存
     * <p> 在服务被销毁时调用, 清除所有缓存的上下文数据, 释放资源.</p>
     */
    @Override
    public void dispose() {
        cache.clear();
    }

    /**
     * 根据上下文信息和用户输入构建增强的提示词字符串
     * <p>该方法将当前目录的环境信息 (如操作系统,Shell,Git 状态, 技术栈等) 与用户输入拼接成结构化提示词, 用于增强 AI 交互或终端辅助功能.</p>
     * <p>输出格式示例:</p>
     * <pre>{@code
     * 当前目录上下文:
     * -os: linux
     * -shell: bash
     * -current-directory: /home/user/project
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
        sb.append("当前目录上下文:\n");
        sb.append("- os: ").append(context.shellEnv.os).append("\n");
        sb.append("- shell: ").append(context.shellEnv.shell).append("\n");
        if (!StringUtil.isEmptyOrSpaces(context.currentDirectory)) {
            sb.append("- current-directory: ").append(context.currentDirectory).append("\n");
        }
        sb.append("- git: ").append(context.projectInfo.isGit).append("\n");
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
        sb.append("\n用户需求:\n");
        sb.append(userInput);
        return sb.toString();
    }

    /**
     * 解析目录路径为 VirtualFile 对象
     * <p> 首先尝试根据当前目录路径查找目录对应的 VirtualFile,
     * 如果未找到则尝试使用项目基础路径进行查找,
     * 如果两者都失败则返回 null</p>
     *
     * @param currentDirectory 当前目录路径, 可能为 null
     * @param projectBasePath  项目基础路径, 可能为 null
     * @return 解析后的 VirtualFile 对象, 如果目录不存在则返回 null
     */
    @Nullable
    private static VirtualFile resolveDirectory(@Nullable String currentDirectory, @Nullable String projectBasePath) {
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        if (!StringUtil.isEmptyOrSpaces(currentDirectory)) {
            VirtualFile currentDir = lfs.findFileByPath(currentDirectory);
            if (currentDir != null) {
                return currentDir;
            }
        }
        if (!StringUtil.isEmptyOrSpaces(projectBasePath)) {
            return lfs.findFileByPath(projectBasePath);
        }
        return null;
    }

    /**
     * 检测当前系统的 Shell 环境信息
     * <p> 通过读取系统环境变量和属性来获取当前 Shell 的类型和操作系统名称,
     * 并返回包含这些信息的 ShellEnv 对象. 如果环境变量中未设置 SHELL,
     * 则使用默认值 "bash".</p>
     *
     * @return ShellEnv 包含 shell 名称和操作系统名称的对象, 永不为 null
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
     * 从给定的 shell 路径中提取简化的 shell 类型名称
     * <p> 方法将输入路径转换为小写字符串, 并检查是否包含 "zsh","bash" 或 "fish".
     * 如果路径中包含这些关键字, 则返回对应的 shell 名称; 否则返回默认的 "bash".
     *
     * @param shellPath 原始 shell 路径字符串, 不能为 null
     * @return 简化的 shell 类型名称, 如 "zsh","bash","fish" 或默认的 "bash"
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
     * 检测项目的技术栈和配置信息
     * <p> 通过检查基础目录中的特定文件来确定项目的技术特性, 包括版本控制, 容器化和编排工具等 </p>
     *
     * @param baseDir 项目的基础目录
     * @return 包含项目信息的 ProjectInfo 对象, 包括是否使用 Git,Dockerfile,Docker Compose 和 Kubernetes
     */
    @NotNull
    private static ProjectInfo detectProjectInfo(@NotNull VirtualFile baseDir) {
        boolean isGit = baseDir.findChild(".git") != null;
        boolean hasDockerfile = baseDir.findChild("Dockerfile") != null;
        boolean hasDockerCompose = baseDir.findChild("docker-compose.yml") != null
                                   || baseDir.findChild("docker-compose.yaml") != null;
        boolean hasKubernetes = false;
        if (baseDir.findChild("k8s") != null
            || baseDir.findChild("manifests") != null
            || baseDir.findChild(".kube") != null
            || baseDir.findChild(".helm") != null) {
            hasKubernetes = true;
        } else {
            for (VirtualFile child : baseDir.getChildren()) {
                String name = child.getName().toLowerCase(Locale.ROOT);
                if ((name.endsWith(".yaml") || name.endsWith(".yml"))
                    && (name.contains("deployment")
                        || name.contains("service")
                        || name.contains("configmap")
                        || name.contains("ingress")
                        || name.contains("k8s"))) {
                    hasKubernetes = true;
                    break;
                }
            }
        }
        return new ProjectInfo(isGit, hasDockerfile, hasDockerCompose, hasKubernetes);
    }

    /**
     * 根据项目根目录检测技术栈信息
     * <p> 该方法通过检查根目录下的关键文件来确定项目使用的构建工具与编程语言. 检测顺序为:</p>
     * <pre>{@code
     * if (baseDir.findChild("pom.xml") != null) {
     *     // Maven
     * } else if (baseDir.findChild("build.gradle") != null
     *          || baseDir.findChild("build.gradle.kts") != null) {
     *     // Gradle
     * } else if (baseDir.findChild("package.json") != null) {
     *     // Node.js
     * } else if (baseDir.findChild("go.mod") != null) {
     *     // Go
     * } else if (baseDir.findChild("pyproject.toml") != null
     *          || baseDir.findChild("requirements.txt") != null) {
     *     // Python
     * }
     * }</pre>
     * 若未匹配到任何文件, 构建工具和语言均返回 {@code null}.
     *
     * @param baseDir 项目根目录
     * @return 包含检测到的构建工具和编程语言的 {@link TechStack}, 若未检测到则相应字段为 {@code null}
     */
    @NotNull
    private static TechStack detectTechStack(@NotNull VirtualFile baseDir) {
        String buildTool = null;
        String language = null;
        if (baseDir.findChild("pom.xml") != null) {
            buildTool = "maven";
            language = "java";
        } else if (baseDir.findChild("build.gradle") != null
                   || baseDir.findChild("build.gradle.kts") != null) {
            buildTool = "gradle";
            language = "java";
        } else if (baseDir.findChild("package.json") != null) {
            buildTool = "npm";
            language = "javascript";
        } else if (baseDir.findChild("go.mod") != null) {
            buildTool = "go";
            language = "go";
        } else if (baseDir.findChild("pyproject.toml") != null
                   || baseDir.findChild("requirements.txt") != null) {
            buildTool = "pip";
            language = "python";
        }
        return new TechStack(buildTool, language);
    }

    /**
     * 终端上下文信息记录
     * <p> 封装了终端环境, 项目状态, 技术栈以及当前目录等信息, 供 {@link TerminalContextService} 生成用户提示词时使用.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.21
     * @since x.x.x
     */
    public record ContextInfo(@NotNull ShellEnv shellEnv,
                              @NotNull ProjectInfo projectInfo,
                              @NotNull TechStack techStack,
                              @Nullable String currentDirectory) {
    }

    /**
     * Shell 环境信息记录类
     * <p> 用于封装终端的 Shell 类型和操作系统名称.
     * 该记录主要用于存储环境检测的结果, 以便在构建上下文提示词时使用.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.21
     * @since x.x.x
     */
    public record ShellEnv(@NotNull String shell, @NotNull String os) {
    }

    /**
     * 项目信息记录类
     * <p> 用于封装当前项目目录的环境特征信息, 包括是否为 Git 仓库, 是否包含 Docker 相关文件, 是否包含 Kubernetes 配置等.</p>
     * <p> 该类为不可变数据类, 适用于在终端上下文服务中传递项目元数据, 支持缓存和快速比较.</p>
     * <p> 提供空实例构造方法 {@code empty()}, 用于初始化默认无特征的项目信息.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.21
     * @since 1.0.0
     */
    public record ProjectInfo(boolean isGit,
                              boolean hasDockerfile,
                              boolean hasDockerCompose,
                              boolean hasKubernetes) {
        /**
         * 创建一个空的 ProjectInfo 实例
         * <p> 该实例表示项目中不包含 Git,Dockerfile,Docker Compose 或 Kubernetes 任何相关配置
         *
         * @return 所有属性均为 false 的 ProjectInfo 对象
         */
        @NotNull
        public static ProjectInfo empty() {
            return new ProjectInfo(false, false, false, false);
        }
    }

    /**
     * 技术栈信息记录类
     * <p> 用于描述项目中使用的技术栈, 包括构建工具和编程语言. 该类为只读数据结构.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email mailto:dong4j@gmail.com
     * @date 2026.01.21
     * @since 1.0.0
     */
    public record TechStack(@Nullable String buildTool, @Nullable String language) {
        /**
         * 创建一个空的 TechStack 实例
         * <p> 该方法返回一个所有属性都为 null 的 TechStack 对象, 用于表示没有配置任何技术栈的情况.
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
     * <p> 用于缓存终端上下文信息, 包含项目信息和技术栈信息, 以提高性能.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.21
     * @since 1.0.0
     */
    private record CachedContext(@NotNull ProjectInfo projectInfo, @NotNull TechStack techStack) {
    }
}
