package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;


/**
 * GitCliff 二进制解析器类
 * <p> 用于解析和设置 Git-Cliff 工具的二进制路径，支持 macOS、Windows 和 Linux 系统。
 * 该类提供了静态方法来检查和设置 Git-Cliff 的可执行权限。
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
     * GitCliffBinaryResolver 的私有构造函数
     * <p> 此构造函数被声明为私有, 用于防止外部实例化该类. 通常用于单例模式或其他需要控制对象创建的场景.
     */
    private GitCliffBinaryResolver() {
    }

    /**
     * 解析并返回 Git-cliff 二进制文件的路径
     * <p> 根据操作系统类型自动检测二进制文件路径，支持 macOS、Windows 和 Linux。
     * 首先检查指定路径是否存在, 若存在则尝试将其设置为可执行文件（非 Windows 系统）。
     *
     * @return Git-cliff 二进制文件的路径, 如果路径不存在则返回 null
     */
    @Nullable
    public static Path resolve() {
        Path binary = GitCliffDownloadManager.getBinaryPath();
        if (binary == null || !Files.exists(binary)) {
            return null;
        }

        // 确保可执行（非 Windows 系统）
        if (!SystemInfo.isWindows) {
            try {
                binary.toFile().setExecutable(true);
            } catch (Exception e) {
                log.debug("Git-cliff 二进制设置可执行权限失败: " + binary, e);
            }
        }
        return binary;
    }
}
