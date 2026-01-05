package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import org.jetbrains.annotations.NotNull;

/**
 * NextEditCandidate 类
 * <p> 用于表示编辑候选项的信息, 包含起始索引, 结束索引, 行号, 得分以及预览内容等属性.
 * <p> 该类主要用于在文本编辑或代码补全场景中, 记录和管理可能的编辑候选项.
 *
 * @param startIndex 起始索引
 * @param endIndex   结束索引
 * @param line       编辑候选的行号
 * @param score      评分值, 用于衡量编辑候选的优先级或匹配度
 * @param preview    预览文本内容, 用于展示编辑建议的预览效果
 * @param source     候选来源标识, 如 "psi" / "text" / "exact"
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
record NextEditCandidate(int startIndex, int endIndex, int line, double score, String preview, String source) {
    /**
     * 初始化下一个编辑候选对象
     * <p> 用于封装编辑位置, 行号, 评分及预览内容等信息, 常用于代码编辑器中高亮或推荐修改位置
     *
     * @param startIndex 起始位置索引
     * @param endIndex   结束位置索引
     * @param line       编辑所在的行号
     * @param score      评分, 用于排序或筛选候选项
     * @param preview    预览文本内容, 不能为空
     * @param source     来源标识, 不能为空
     * @since 1.0
     */
    NextEditCandidate(int startIndex, int endIndex, int line, double score, @NotNull String preview, @NotNull String source) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.line = line;
        this.score = score;
        this.preview = preview;
        this.source = source;
    }

    /**
     * 获取起始索引
     * <p> 返回该编辑候选对象的起始索引位置.
     *
     * @return 起始索引
     */
    @Override
    public int startIndex() {
        return startIndex;
    }

    /**
     * 获取结束索引位置
     * <p> 返回当前编辑候选区域的结束字符索引, 用于标识该候选区域在文本中的结束位置
     *
     * @return 结束索引值
     */
    @Override
    public int endIndex() {
        return endIndex;
    }

    /**
     * 获取候选编辑项所在的行号
     * <p> 返回该编辑候选项对应代码中的行号信息
     *
     * @return 行号
     */
    @Override
    public int line() {
        return line;
    }

    /**
     * 获取编辑候选的评分值
     * <p> 返回该编辑候选的评分, 评分用于表示该候选的优先级或相关性
     *
     * @return 评分值, 范围未指定, 具体取决于评分计算逻辑
     */
    @Override
    public double score() {
        return score;
    }

    /**
     * 获取预览内容
     * <p> 返回当前编辑候选对象的预览文本内容
     *
     * @return 预览文本内容, 非空字符串
     */
    @Override
    @NotNull
    public String preview() {
        return preview;
    }

    /**
     * 获取候选来源标识
     * <p> 返回该候选是由 PSI 分析还是文本相似度产生
     *
     * @return 来源标识, 如 "psi" 或 "text"
     */
    @Override
    @NotNull
    public String source() {
        return source;
    }
}
