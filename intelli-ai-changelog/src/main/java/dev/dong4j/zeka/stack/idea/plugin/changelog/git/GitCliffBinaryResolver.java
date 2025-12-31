package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;


/**
 * GitCliff 二进制解析器类
 * <p> 用于在 macOS 系统上解析和设置 Git-Cliff 工具的二进制路径. 该类提供了静态方法来检查和设置 Git-Cliff 的可执行权限.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public final class GitCliffBinaryResolver {

    /**
     * 记录器实例, 用于在 GitCliffBinaryResolver 类中记录日志信息
     *
     * @see Logger
     */
    private static final Logger log = Logger.getInstance(GitCliffBinaryResolver.class);
    /**
     * macOS 平台上 Git Cliff 二进制文件的路径
     * <p> 此路径用于在 macOS 系统中定位 Git Cliff 工具的安装位置
     */
    private static final String MAC_BINARY_PATH = "/Users/dong4j/.zeka-stack/plugin/changelog/git-cliff-2.11.0/git-cliff";

    /**
     * GitCliffBinaryResolver 的私有构造函数
     * <p> 此构造函数被声明为私有, 用于防止外部实例化该类. 通常用于单例模式或其他需要控制对象创建的场景.
     */
    private GitCliffBinaryResolver() {
    }

    /**
     * 解析并返回 Git-cliff 二进制文件的路径
     * <p> 此方法仅在系统为 macOS 时有效. 首先检查指定路径是否存在, 若存在则尝试将其设置为可执行文件.
     * 若路径不存在或系统不是 macOS, 则返回 null.
     *
     * @return Git-cliff 二进制文件的路径, 如果路径不存在或系统不是 macOS 则返回 null
     */
    @Nullable
    public static Path resolve() {
        if (!SystemInfo.isMac) {
            return null;
        }

        Path binary = Path.of(MAC_BINARY_PATH);
        if (!Files.exists(binary)) {
            return null;
        }

        // 确保可执行（macOS）
        try {
            binary.toFile().setExecutable(true);
        } catch (Exception e) {
            log.warn("Git-cliff 二进制设置可执行权限失败: " + binary, e);
        }
        return binary;
    }
}
