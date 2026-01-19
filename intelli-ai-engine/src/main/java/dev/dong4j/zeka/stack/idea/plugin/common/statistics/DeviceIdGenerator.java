package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 设备 ID 生成器
 * <p>
 * 使用硬件/系统信息生成稳定的 deviceId。
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public final class DeviceIdGenerator {

    /**
     * 私有构造函数, 防止外部实例化
     * <p>
     * 该类为工具类, 所有方法均为静态, 不可被实例化
     */
    private DeviceIdGenerator() {
    }

    /**
     * 生成设备 ID
     *
     * @return 设备 ID
     */
    @NotNull
    public static String generateDeviceId() {
        String raw = firstNonEmpty(
            readLinuxMachineId(),
            readMacPlatformUuid(),
            readWindowsMachineGuid(),
            readMacAddresses()
                                  );

        if (raw == null || raw.isBlank()) {
            // 最后兜底：随机 UUID（仅在无法获取任何指纹信息时使用）
            return UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
        }

        return toUuidLike(sha256Hex(raw));
    }

    /**
     * 将设备 ID 转换为适合在文件路径中使用的安全格式
     * <p> 移除所有非字母数字, 下划线和连字符的字符, 替换为下划线, 并在结果为空时返回 "unknown"
     *
     * @param deviceId 需要被清理的设备 ID 字符串, 不能为空
     * @return 安全的路径格式字符串, 若清理后为空则返回 "unknown"
     */
    @NotNull
    public static String sanitizeForPath(@NotNull String deviceId) {
        String safe = deviceId.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        return safe.isEmpty() ? "unknown" : safe;
    }

    /**
     * 从 Linux 系统读取机器 ID
     * <p> 优先尝试读取文件 <code>/etc/machine-id</code>, 若不存在或为空, 则尝试读取 <code>/var/lib/dbus/machine-id</code>, 返回第一个非空值 </p>
     *
     * @return 读取到的机器 ID, 若文件不存在或内容为空则返回 null
     */
    @Nullable
    private static String readLinuxMachineId() {
        String id = readFile("/etc/machine-id");
        if (id == null || id.isBlank()) {
            id = readFile("/var/lib/dbus/machine-id");
        }
        return id;
    }

    /**
     * 从 macOS 平台读取设备唯一标识符 (UUID)
     * <p> 通过执行系统命令 <pre>{@code ioreg -rd1 -c IOPlatformExpertDevice}</pre> 并提取其中的 <code>IOPlatformUUID</code> 字段值
     *
     * @return 设备唯一标识符字符串, 如果提取失败或命令执行异常则返回 null
     */
    @Nullable
    private static String readMacPlatformUuid() {
        return runCommandAndExtract(new String[] {"ioreg", "-rd1", "-c", "IOPlatformExpertDevice"}, "IOPlatformUUID");
    }

    /**
     * 从 Windows 注册表读取机器 GUID
     * <p> 通过执行命令 <pre>{@code reg query HKLM\\SOFTWARE\\Microsoft\\Cryptography /v MachineGuid}</pre> 并提取 "MachineGuid" 键值获取设备唯一标识符
     *
     * @return 机器 GUID 字符串, 如果读取失败或键值不存在则返回 null
     */
    @Nullable
    private static String readWindowsMachineGuid() {
        return runCommandAndExtract(
            new String[] {"reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid"},
            "MachineGuid"
                                   );
    }

    /**
     * 读取设备所有网卡的 MAC 地址并拼接成字符串
     * <p> 遍历系统中所有网络接口, 获取其硬件地址 (MAC 地址), 并以十六进制字符串形式拼接, 用逗号分隔. 若无有效 MAC 地址或发生异常, 则返回 null.
     *
     * @return 所有有效 MAC 地址拼接后的字符串, 格式为 "mac1,mac2,mac3...", 若无有效地址或发生异常则返回 null
     */
    @Nullable
    private static String readMacAddresses() {
        try {
            List<String> macs = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return null;
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac == null || mac.length == 0) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) {
                    sb.append(String.format("%02x", b));
                }
                macs.add(sb.toString());
            }
            return macs.isEmpty() ? null : String.join(",", macs);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 执行命令并从输出中提取指定键对应的值
     * <p> 通过执行给定命令, 读取其标准输出, 逐行查找包含指定键的行, 并提取该行最后一个非空字段作为结果. 若执行失败或未找到匹配内容, 则返回 null.
     *
     * @param command 用于执行的命令数组, 例如 {@code new String[]{"ls", "-l"}}, 必须非空
     * @param key     要查找的键字符串, 用于匹配输出行, 必须非空
     * @return 匹配行中最后一个非空字段的值 (去除引号和空白), 若未找到或执行失败则返回 null
     */
    @Nullable
    private static String runCommandAndExtract(@NotNull String[] command, @NotNull String key) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(key)) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length > 0) {
                            return parts[parts.length - 1].replace("\"", "").trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    /**
     * 从指定路径读取文件的第一行内容
     * <p> 尝试以 UTF-8 编码读取文件, 若文件不存在或读取过程中发生异常, 则返回 null
     *
     * @param path 文件路径, 不能为空
     * @return 文件第一行内容, 若文件不存在或读取失败则返回 null
     */
    @Nullable
    private static String readFile(@NotNull String path) {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 对输入字符串进行 SHA-256 哈希计算并返回十六进制字符串
     * <p> 使用 SHA-256 算法对输入字符串进行哈希处理, 结果以小写十六进制格式返回. 若计算过程中发生异常, 则回退生成一个随机 UUID 字符串 (去除连字符).
     *
     * @param input 需要哈希的输入字符串, 不能为空
     * @return SHA-256 哈希结果的十六进制字符串, 长度为 32 位 (小写)
     */
    @NotNull
    private static String sha256Hex(@NotNull String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 理论不会发生
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * 将十六进制字符串格式化为类似 UUID 的格式
     * <p> 将输入的十六进制字符串截取或填充至 32 位, 然后按 UUID 格式分段拼接为标准 UUID 样式字符串 </p>
     * <p> 示例: 输入 <code>{@code abc}</code>, 输出 <code>{@code 00000000-0000-0000-0000-000000000000}</code>; 输入 <code>{@code 123456789abcdef
     * }</code>, 输出 <code>{@code 12345678-9abc-def0-0000-000000000000}</code></p>
     *
     * @param hex 十六进制字符串, 长度至少为 0, 建议为 32 位
     * @return 格式化后的类似 UUID 的字符串, 长度固定为 36 个字符 (含 4 个连字符)
     */
    @NotNull
    private static String toUuidLike(@NotNull String hex) {
        String h = hex.length() >= 32 ? hex.substring(0, 32) : String.format("%1$-32s", hex).replace(' ', '0');
        return h.substring(0, 8) + "-" +
               h.substring(8, 12) + "-" +
               h.substring(12, 16) + "-" +
               h.substring(16, 20) + "-" +
               h.substring(20, 32);
    }

    /**
     * 返回第一个非空且非空白的字符串值
     * <p> 遍历传入的字符串数组, 返回第一个满足非 null 且非空白的字符串. 若数组为 null 或所有元素均为空, 则返回 null.</p>
     *
     * @param values 字符串数组, 可变参数, 允许传入零个或多个字符串
     * @return 第一个非空非空白的字符串, 若不存在则返回 null
     */
    @Nullable
    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
