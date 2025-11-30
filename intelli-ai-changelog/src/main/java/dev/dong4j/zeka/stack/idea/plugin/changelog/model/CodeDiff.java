package dev.dong4j.zeka.stack.idea.plugin.changelog.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 代码差异信息类
 * <p>
 * 用于表示代码文件的变更信息, 包括文件路径, 变更类型, 新增 / 删除行数以及具体的差异内容.
 * 该类常用于代码版本对比, 代码审查或变更日志记录等场景.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class CodeDiff {
    /** 文件路径 */
    @NotNull
    public String filePath;
    /** 变更类型 */
    @NotNull
    public ChangeType changeType;
    /** 新增行数 */
    public int addedLines;
    /** 删除行数 */
    public int deletedLines;
    /** Diff 内容 */
    @Nullable
    public String diffContent;

    /**
     * 构造一个 CodeDiff 对象, 用于表示文件的修改差异信息
     * <p>
     * 该构造函数用于初始化文件路径, 修改类型, 新增行数, 删除行数以及差异内容
     *
     * @param filePath     文件路径
     * @param changeType   修改类型
     * @param addedLines   新增行数
     * @param deletedLines 删除行数
     * @param diffContent  差异内容, 可能为 null
     */
    public CodeDiff(@NotNull String filePath,
                    @NotNull ChangeType changeType,
                    int addedLines,
                    int deletedLines,
                    @Nullable String diffContent) {
        this.filePath = filePath;
        this.changeType = changeType;
        this.addedLines = addedLines;
        this.deletedLines = deletedLines;
        this.diffContent = diffContent;
    }

    /**
     * 变更类型枚举
     * <p>
     * 用于表示文件或对象在系统中发生的变更类型, 包括新增, 删除, 修改, 重命名和移动等操作
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.30
     * @since 1.0.0
     */
    public enum ChangeType {
        /** 新增文件 */
        ADD,
        /** 删除文件 */
        DELETE,
        /** 修改文件 */
        MODIFY,
        /** 重命名文件 */
        RENAME,
        /** 移动文件 */
        MOVE
    }
}

