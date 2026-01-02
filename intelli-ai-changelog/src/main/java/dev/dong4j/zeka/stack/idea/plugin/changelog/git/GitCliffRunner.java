package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogToolWindowService;
import dev.dong4j.zeka.stack.idea.plugin.kit.MessageFormatter;

/**
 * GitCliffRunner 类
 * <p> 该类用于执行 git-cliff 工具以生成变更日志. 通过调用静态方法 run 可以启动 git-cliff 进程, 并获取其输出结果.
 * <p>run 方法接受二进制文件路径, 工作目录, 配置字符串, 参数列表和输出会话作为参数, 返回 git-cliff 的标准输出.
 * <p> 如果 git-cliff 执行失败, 则抛出异常.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public final class GitCliffRunner {

    /**
     * Git Cliff Runner 的私有构造函数
     * <p> 此构造函数被声明为私有, 以防止外部实例化该类. 所有操作应通过静态方法进行.
     */
    private GitCliffRunner() {
    }

    /**
     * 运行 git-cliff 工具并返回其输出结果
     * <p> 该方法启动一个 git-cliff 进程, 执行指定的命令, 并将输出结果返回. 如果进程退出码非零, 则抛出异常.
     *
     * @param binary        git-cliff 可执行文件的路径
     * @param workingDir    工作目录路径
     * @param config        可选的配置字符串, 如果提供且不为空白, 则创建临时配置文件
     * @param args          命令行参数列表
     * @param outputSession 输出会话对象, 用于实时输出进程的标准输出
     * @return git-cliff 的标准输出结果
     * @throws Exception 当 git-cliff 执行失败时抛出
     */
    @NotNull
    public static String run(@NotNull Path binary,
                             @NotNull Path workingDir,
                             @Nullable String config,
                             @NotNull List<String> args,
                             @NotNull ChangelogToolWindowService.ChangelogOutputSession outputSession) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(binary.toString());

        if (config != null && !config.isBlank()) {
            // 将配置写入临时文件，避免依赖外部文件
            Path configPath = Files.createTempFile("git-cliff-", ".toml");
            Files.writeString(configPath, config, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            configPath.toFile().deleteOnExit();
            command.add("-c");
            command.add(configPath.toString());
        }

        if (!args.isEmpty()) {
            command.addAll(args);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();
        StringBuilder output = new StringBuilder();

        try (InputStream input = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, read, StandardCharsets.UTF_8);
                output.append(chunk);
                // 流式输出原始内容，最后统一格式化
                outputSession.append(chunk);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("git-cliff 执行失败，退出码: " + exitCode);
        }

        String result = output.toString();
        // 格式化输出结果
        String formattedResult = MessageFormatter.format(result);
        // 更新 toolwindow 中的最终结果（替换流式输出的原始内容）
        outputSession.setText(formattedResult);
        return formattedResult;
    }
}
