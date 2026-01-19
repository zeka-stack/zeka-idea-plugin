package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import lombok.Getter;

/**
 * <p>Description : 统计触发入口枚举.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
@Getter
public enum StatisticsUserAction {

    /** 项目树-右键菜单-目录 */
    PROJECT_TREE_CONTEXT_MENU_DIR("project_tree_context_menu_dir", "项目树-右键菜单-目录"),
    /** 项目树-右键菜单-文件 */
    PROJECT_TREE_CONTEXT_MENU_FILE("project_tree_context_menu_file", "项目树-右键菜单-文件"),
    /** 编辑器-右键菜单 */
    EDITOR_CONTEXT_MENU("editor_context_menu", "编辑器-右键菜单"),
    /** 项目树-快捷键-目录 */
    PROJECT_TREE_SHORTCUT_DIR("project_tree_shortcut_dir", "项目树-快捷键-目录"),
    /** 项目树-快捷键-文件 */
    PROJECT_TREE_SHORTCUT_FILE("project_tree_shortcut_file", "项目树-快捷键-文件"),
    /** 编辑器-快捷键 */
    EDITOR_SHORTCUT("editor_shortcut", "编辑器-快捷键"),
    /** 提交面板 */
    COMMIT_PANEL("commit_panel", "提交面板"),
    /** Git 日志面板 */
    GIT_LOG_PANEL("git_log_panel", "Git 日志面板"),
    /** 意图动作入口 */
    INTENTION("intention", "意图动作"),
    /** 状态栏入口 */
    STATUS_BAR("status_bar", "状态栏"),
    /** 其他/未知入口 */
    UNKNOWN("unknown", "未知");

    /** 枚举代码值 */
    private final String code;
    /** 枚举描述 */
    private final String description;

    /**
     * 构造函数, 初始化统计用户操作枚举项的代码值和描述
     *
     * @param code        枚举项的代码值, 用于唯一标识该操作
     * @param description 枚举项的描述信息, 用于用户友好展示
     */
    StatisticsUserAction(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码值获取对应的统计用户操作枚举项
     * <p> 通过传入的代码值在枚举常量中查找匹配项, 若未找到则返回默认的 UNKNOWN 枚举项 </p>
     *
     * @param code 用于匹配的枚举代码值, 若为 null 或空字符串则返回 {@link StatisticsUserAction#UNKNOWN}
     * @return 匹配的 {@link StatisticsUserAction} 枚举项, 若未找到则返回 {@link StatisticsUserAction#UNKNOWN}
     */
    public static StatisticsUserAction fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return UNKNOWN;
        }
        for (StatisticsUserAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return UNKNOWN;
    }
}
