package dev.dong4j.zeka.stack.idea.plugin.util;

import com.intellij.util.EnvironmentUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统工具类
 * <p>
 * 提供与操作系统和环境相关的静态工具方法, 用于获取系统属性, 判断操作系统类型等.
 * 包含获取用户目录, 用户名称, 系统名称等系统信息的方法, 并支持通过枚举方式判断当前操作系统是否为 Linux,Mac 或 Windows.
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@Slf4j
@UtilityClass
public class SystemUtils {
    /** 用户主目录路径, 从系统属性 "user.home" 获取 */
    @Nullable
    public static final String USER_HOME = getProperty("user.home");
    /**
     * 用户目录路径
     * <p>
     * 通过系统属性 "user.dir" 获取当前用户的工作目录
     *
     * @see java.lang.System#getProperty(String)
     */
    @Nullable
    public static final String USER_DIR = getProperty("user.dir");
    /** 系统用户名称属性值, 若未设置则为 null */
    @Nullable
    public static final String USER_NAME = getProperty("user.name");
    /** 操作系统名称, 可能为 null */
    @Nullable
    public static final String OS_NAME = getProperty("os.name");

    /**
     * 根据指定属性名获取对应的属性值, 优先从环境变量中获取, 其次为系统属性, 最后为环境工具类中的值
     * <p>
     * 该方法会依次尝试从环境变量, 系统属性和环境工具类中获取属性值, 若所有来源均未找到, 则返回 null
     *
     * @param property 属性名
     * @return 属性值, 若未找到则返回 null
     */
    @Nullable
    public static String getProperty(String property) {
        String value = System.getenv(property);
        return StringUtils.isBlank(value)
               ? StringUtils.isBlank(System.getProperty(property))
                 ? EnvironmentUtil.getValue(property)
                 : System.getProperty(property)
               : value;
    }

    /**
     * 判断当前操作系统是否为 Linux 系统
     * <p>
     * 通过检查操作系统名称是否包含 "LINUX" 来确定当前系统是否为 Linux
     *
     * @return 如果操作系统为 Linux, 则返回 true, 否则返回 false
     */
    public static boolean isLinux() {
        return StringUtils.isNotBlank(OS_NAME) && OS_NAME.toUpperCase().contains(OsType.LINUX.name());
    }

    /**
     * 判断当前操作系统是否为 macOS
     * <p>
     * 通过检查系统名称是否包含 "MAC" 来判断当前操作系统类型
     *
     * @return 如果当前操作系统是 macOS, 返回 true; 否则返回 false
     */
    public static boolean isMac() {
        return StringUtils.isNotBlank(OS_NAME) && OS_NAME.toUpperCase().contains(OsType.MAC.name());
    }

    /**
     * 判断当前操作系统是否为 Windows 系统
     * <p>
     * 通过检查系统名称是否包含 "WINDOWS" 来判断当前操作系统类型
     *
     * @return 如果当前操作系统是 Windows 系统, 返回 true; 否则返回 false
     */
    public static boolean isWindows() {
        return StringUtils.isNotBlank(OS_NAME) && OS_NAME.toUpperCase().contains(OsType.WINDOWS.name());
    }

    /**
     * 操作系统类型枚举
     * <p>
     * 用于表示不同的操作系统类型, 包含 Linux,Mac 和 Windows 三种常见操作系统类型.
     * 该枚举常用于系统兼容性判断, 环境配置识别等场景.
     *
     * @author dong4j
     * @version 1.0.0
     * @date 2025.10.24
     * @since 1.0.0
     */
    public enum OsType {
        /** Linux 操作系统标识 */
        LINUX,
        /** 宏定义常量 */
        MAC,
        /** 操作系统类型, 表示 Windows 系统 */
        WINDOWS
    }
}
