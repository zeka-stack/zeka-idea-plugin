package dev.dong4j.zeka.stack.idea.plugin.repairer.violation;

/**
 * SeverityMapper
 * <p> 该工具类提供静态方法用于将 Checkstyle 或 PMD 报告中的严重程度字符串或优先级转换为统一的数值表示.<p>
 * 通过 {@link #fromCheckstyle(String)} 根据 Checkstyle 的 severity(error,warning,info 等) 返回对应的整数级别, 缺省为 3(中等).<p>
 * 通过 {@link #fromPmdPriority(String)} 根据 PMD 的优先级字符串将其解析为整数等级, 同样缺省或解析失败返回 3.<p>
 * <pre>{@code
 * // Checkstyle severity to level
 * Error   -> 1
 * Warning -> 2
 * Info    -> 4
 * Others  -> 3
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since x.x.x
 */
public final class SeverityMapper {
    /**
     * 私有构造函数
     * <p> 用于防止外部实例化当前工具类
     */
    private SeverityMapper() {
    }

    /**
     * 将 Checkstyle 的严重级别字符串转换为对应的整数表示
     * <p> 如果输入的严重级别为 null, 则返回默认值 3. 根据输入的字符串值, 返回对应的整数值:
     * "error" 对应 1,"warning" 对应 2,"info" 对应 4, 其他情况返回默认值 3.
     *
     * @param severity 严重级别字符串, 可能为 null
     * @return 对应的整数表示, 范围为 1 到 4, 默认值为 3
     */
    public static int fromCheckstyle(String severity) {
        if (severity == null) {
            return 3;
        }
        return switch (severity.toLowerCase()) {
            case "error" -> 1;
            case "warning" -> 2;
            case "info" -> 4;
            default -> 3;
        };
    }

    /**
     * 将 PMD 优先级字符串转换为整数优先级
     * <p> 如果输入的优先级字符串为 null, 则返回默认值 3; 否则尝试解析为整数, 若解析失败则也返回 3</p>
     *
     * @param priority PMD 优先级字符串, 可能为 null
     * @return 解析后的整数优先级, 失败或 null 时返回 3
     */
    public static int fromPmdPriority(String priority) {
        if (priority == null) {
            return 3;
        }
        try {
            return Integer.parseInt(priority);
        } catch (NumberFormatException e) {
            return 3;
        }
    }
}
