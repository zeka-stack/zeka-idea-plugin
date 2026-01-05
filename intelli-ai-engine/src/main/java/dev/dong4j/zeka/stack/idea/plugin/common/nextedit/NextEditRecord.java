package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import org.jetbrains.annotations.NotNull;

/**
 * 编辑记录类
 * <p> 用于记录用户在文本编辑过程中所做的修改, 包括修改的起始位置, 结束位置, 旧文本内容, 新文本内容以及修改时间戳
 *
 * @param startOffset 编辑起始位置的偏移量
 * @param endOffset   结束偏移量
 * @param oldText     旧文本内容, 用于记录编辑前的原始文本
 * @param newText     新文本内容
 *                    <p> 表示编辑操作后的新文本内容
 * @param timestamp   编辑记录的时间戳, 表示此次编辑操作发生的时间
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
record NextEditRecord(int startOffset, int endOffset, String oldText, String newText, long timestamp) {
    /**
     * 构造一个 NextEditRecord 对象, 用于记录编辑操作的详细信息
     * <p> 该构造函数初始化编辑的起始偏移量, 结束偏移量, 旧文本, 新文本以及时间戳
     *
     * @param startOffset 编辑操作的起始偏移量
     * @param endOffset   编辑操作的结束偏移量
     * @param oldText     编辑前的文本内容, 不能为空
     * @param newText     编辑后的文本内容, 不能为空
     * @param timestamp   编辑操作发生的时间戳
     */
    NextEditRecord(int startOffset, int endOffset, @NotNull String oldText, @NotNull String newText, long timestamp) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.oldText = oldText;
        this.newText = newText;
        this.timestamp = timestamp;
    }

    /**
     * 获取编辑记录的起始偏移量
     * <p> 返回该编辑记录在文本中的起始位置偏移量
     *
     * @return 起始偏移量
     */
    @Override
    public int startOffset() {
        return startOffset;
    }

    /**
     * 获取编辑记录的结束偏移量
     * <p> 返回该编辑记录在文本中的结束位置偏移量
     *
     * @return 编辑记录的结束偏移量
     */
    @Override
    public int endOffset() {
        return endOffset;
    }

    /**
     * 获取旧文本
     * <p> 返回与当前编辑记录相关的旧文本
     *
     * @return 旧文本, 不能为空
     */
    @Override
    @NotNull
    public String oldText() {
        return oldText;
    }

    /**
     * 获取新文本内容
     * <p> 返回此编辑记录中替换后的文本内容
     *
     * @return 新文本内容, 永远不为 null
     */
    @Override
    @NotNull
    public String newText() {
        return newText;
    }

    /**
     * 获取当前编辑记录的时间戳
     * <p> 返回记录创建时的时间戳
     *
     * @return 时间戳
     */
    @Override
    public long timestamp() {
        return timestamp;
    }
}
