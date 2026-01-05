package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * 下一个编辑跟踪器类
 * <p>用于在编辑器中跟踪用户最近的编辑操作, 并基于编辑内容智能推荐后续可能的编辑建议.
 * <p>该类通过监听文档变更和光标移动事件, 记录编辑内容, 延迟触发候选建议计算, 并按优先级展示建议项.
 * <p>支持去抖动机制 (debounce) 以避免频繁计算, 最多保留 8 个候选项, 支持动态调整建议偏移量.
 * <p>使用示例:
 * <pre>{@code
 * NextEditTracker tracker = new NextEditTracker(project, editor);
 * // 后续通过 acceptSuggestion()或 rejectSuggestion() 控制建议的采纳或取消
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class NextEditTracker implements Disposable {
    /** 用于防抖触发的键名, 标识 NextEdit 操作的唯一标识符 */
    private static final String DEBOUNCE_KEY = "nextedit";
    /** 最大候选建议数量, 用于限制每次计算的编辑建议上限 */
    private static final int MAX_CANDIDATES = 8;

    /** 项目实例, 用于获取项目相关资源和配置 */
    private final Project project;
    /** 编辑器实例, 用于监听文档变化和光标位置变动 */
    private final Editor editor;
    /**
     * 延迟触发器, 用于在编辑操作后延迟计算下一个编辑建议.
     * <p> 通过设置一个延迟时间来避免频繁的建议计算, 确保用户体验流畅.
     *
     * @see NextEditDebouncer
     */
    private final NextEditDebouncer debouncer = new NextEditDebouncer();
    /**
     * 提供候选编辑建议的查找器.
     * <p> 负责在给定的文本中查找可能的编辑建议.
     */
    private final NextEditCandidateFinder candidateFinder = new NextEditCandidateFinder();
    /**
     * 候选建议项队列, 用于存储待显示的编辑建议项
     *
     * @see NextEditSuggestionItem 用于表示具体的文本编辑建议项, 包含起始位置, 结束位置, 替换内容和匹配得分
     */
    private final Queue<NextEditSuggestionItem> suggestionQueue = new ArrayDeque<>();

    /** 最近一次编辑记录, 用于追踪用户输入变更并生成建议 */
    private NextEditRecord lastEdit;
    /**
     * 当前正在显示的编辑建议, 用于用户确认或取消操作.
     *
     * @see #acceptSuggestion() 用于接受当前建议
     * @see #rejectSuggestion() 用于拒绝当前建议
     */
    private NextEditSuggestion currentSuggestion;
    /** 是否正在应用建议, 用于防止递归或重复应用 */
    private boolean applyingSuggestion;

    /** 文档变更监听器, 用于监听编辑器中文档内容的修改并触发后续建议计算逻辑 */
    private final DocumentListener documentListener = new DocumentListener() {
        /**
         * 处理文档内容变化事件
         * <p> 当文档内容发生变化时触发此方法, 用于处理编辑相关的逻辑, 如拒绝建议, 跟踪编辑操作等.
         *
         * @param event 文档事件对象, 包含变化的详细信息
         */
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            if (applyingSuggestion) {
                return;
            }
            rejectSuggestion();
            trackEdit(event);
            if (!NextEditSettings.getInstance().enabled) {
                return;
            }
            if (lastEdit == null) {
                return;
            }
            scheduleTrigger();
        }
    };

    /** 光标位置变化监听器, 用于在有建议时取消当前建议 */
    private final CaretListener caretListener = new CaretListener() {
        /**
         * 当光标位置发生变化时的回调处理
         * <p> 如果当前存在建议项, 则拒绝该建议项, 防止光标移动时保留旧的补全建议
         *
         * @param event 光标事件对象, 包含光标位置变化信息, 不能为 null
         */
        @Override
        public void caretPositionChanged(@NotNull CaretEvent event) {
            if (currentSuggestion != null) {
                rejectSuggestion();
            }
        }
    };

    /**
     * 构造函数, 初始化 NextEditTracker 实例
     *
     * @param project 当前项目实例, 不可为 null
     * @param editor  编辑器实例, 不可为 null
     */
    NextEditTracker(@NotNull Project project, @NotNull Editor editor) {
        this.project = project;
        this.editor = editor;
        attachListeners();
    }

    /**
     * 检查当前是否存在编辑建议
     * <p> 该方法用于判断当前是否有可用的编辑建议.
     *
     * @return 如果存在编辑建议, 则返回 true; 否则返回 false
     */
    boolean hasSuggestion() {
        return currentSuggestion != null;
    }

    /**
     * 接受当前建议的编辑内容
     * <p> 当用户确认接受当前建议时, 执行以下操作:
     * <ul>
     * <li> 将当前建议对象保存到局部变量 </li>
     * <li> 清空当前建议引用 </li>
     * <li> 调用建议对象的 accept 方法执行实际编辑 </li>
     * <li> 调整建议队列中后续建议的偏移量 </li>
     * <li> 显示下一个建议 </li>
     * </ul>
     * <p> 在 finally 块中确保将正在应用建议的标志重置为 false, 防止重复操作.
     *
     * @see NextEditSuggestion#accept() 用于执行实际的编辑操作
     * @see #adjustQueueOffsets(NextEditSuggestion) 用于调整队列中后续建议的偏移量
     * @see #showNextSuggestion() 用于显示下一个建议
     */
    void acceptSuggestion() {
        if (currentSuggestion == null) {
            return;
        }
        applyingSuggestion = true;
        try {
            NextEditSuggestion suggestion = currentSuggestion;
            currentSuggestion = null;
            suggestion.accept();
            adjustQueueOffsets(suggestion);
            showNextSuggestion();
        } finally {
            applyingSuggestion = false;
        }
    }

    /**
     * 拒绝当前的建议
     * <p> 如果存在当前建议, 则释放该建议并将其置为 null, 同时清空建议队列
     */
    void rejectSuggestion() {
        if (currentSuggestion != null) {
            currentSuggestion.dispose();
            currentSuggestion = null;
        }
        suggestionQueue.clear();
    }

    /**
     * 释放资源并清理监听器
     * <p> 调用此方法时, 会拒绝当前建议并移除光标位置监听器, 确保资源被正确释放.
     *
     * @see #rejectSuggestion()
     * @see com.intellij.openapi.editor.CaretListener
     */
    @Override
    public void dispose() {
        rejectSuggestion();
        editor.getCaretModel().removeCaretListener(caretListener);
    }

    /**
     * 注册文档和光标监听器
     * <p> 为编辑器的文档对象添加文档变更监听器, 并为光标模型添加光标位置变化监听器,
     * 以便在用户进行编辑或光标移动时触发相应的处理逻辑.
     */
    private void attachListeners() {
        editor.getDocument().addDocumentListener(documentListener, this);
        editor.getCaretModel().addCaretListener(caretListener);
    }

    /**
     * 记录文档编辑事件, 更新最近一次编辑信息
     * <p> 该方法用于跟踪文档的编辑变化, 当文本内容发生实际变化时, 会创建新的编辑记录并更新最后编辑信息.
     *
     * @param event 文档事件对象, 包含编辑前后的文本片段和偏移量信息
     */
    private void trackEdit(@NotNull DocumentEvent event) {
        String oldText = event.getOldFragment().toString();
        String newText = event.getNewFragment().toString();
        if (oldText.isBlank() || newText.isBlank()) {
            lastEdit = null;
            return;
        }
        if (oldText.equals(newText)) {
            lastEdit = null;
            return;
        }
        int startOffset = event.getOffset();
        int endOffset = startOffset + event.getNewLength();
        lastEdit = new NextEditRecord(startOffset, endOffset, oldText, newText, System.currentTimeMillis());
    }

    /**
     * 调度建议计算触发器
     * <p> 在指定延迟后触发建议计算, 使用去抖动机制避免频繁计算
     * <p> 示例:
     * <pre>{@code
     * scheduleTrigger(); // 在设置的去抖延迟后触发 computeSuggestions
     * }</pre>
     */
    private void scheduleTrigger() {
        long delay = NextEditSettings.getInstance().debounceMs;
        debouncer.debounce(DEBOUNCE_KEY, delay, this::computeSuggestions);
    }

    /**
     * 计算并生成下一步编辑建议
     * <p> 根据最近一次编辑记录查找候选的编辑建议, 并将建议项加入队列中, 最后触发显示下一个建议
     *
     * @since 1.0
     */
    private void computeSuggestions() {
        if (lastEdit == null) {
            return;
        }
        Document document = editor.getDocument();
        String fullText = document.getText();
        List<NextEditCandidate> candidates = candidateFinder.findCandidates(fullText, lastEdit, MAX_CANDIDATES);
        AIConsoleLoggerUtil.print(project, "NextEdit 候选数: " + candidates.size());
        AIConsoleLoggerUtil.print(project, "NextEdit 候选列表:\n" + candidateFinder.toDebugString(candidates));
        suggestionQueue.clear();
        for (NextEditCandidate candidate : sortCandidates(candidates)) {
            suggestionQueue.add(new NextEditSuggestionItem(candidate.startIndex(), candidate.endIndex(),
                                                           lastEdit.newText(), candidate.score()));
        }
        ApplicationManager.getApplication().invokeLater(this::showNextSuggestion);
    }

    /**
     * 显示下一个编辑建议
     * <p> 从建议队列中取出下一个编辑建议, 并显示在编辑器中
     * <p> 如果编辑器已销毁或建议无效, 则继续尝试显示下一个建议
     *
     * @since 1.0
     */
    private void showNextSuggestion() {
        if (editor.isDisposed()) {
            return;
        }
        NextEditSuggestionItem next = suggestionQueue.poll();
        if (next == null) {
            return;
        }
        if (next.startIndex < 0 || next.endIndex > editor.getDocument().getTextLength()) {
            showNextSuggestion();
            return;
        }
        NextEditSuggestion suggestion = new NextEditSuggestion(next.startIndex, next.endIndex, next.replacement, editor);
        currentSuggestion = suggestion;
        suggestion.show();
    }

    /**
     * 调整建议队列中项的偏移量以适应编辑操作
     * <p> 当有新的编辑操作发生时, 该方法会根据编辑的偏移量调整队列中所有相关项的位置, 确保它们在文档中的位置正确.
     *
     * @param suggestion 包含编辑信息的建议对象, 不能为 null
     */
    private void adjustQueueOffsets(@NotNull NextEditSuggestion suggestion) {
        int delta = suggestion.getDelta();
        if (delta == 0) {
            return;
        }
        int changeEnd = suggestion.getEndOffset();
        for (NextEditSuggestionItem item : suggestionQueue) {
            if (item.startIndex >= changeEnd) {
                item.shift(delta);
            }
        }
    }

    /**
     * 对候选编辑建议进行排序
     * <p> 根据候选编辑建议的得分进行降序排列, 并按与最近一次编辑位置的距离进行二次排序
     *
     * @param candidates 候选编辑建议列表, 不能为 null
     * @return 排序后的候选编辑建议列表
     * @since 1.0
     */
    private List<NextEditCandidate> sortCandidates(@NotNull List<NextEditCandidate> candidates) {
        int anchor = lastEdit != null ? lastEdit.startOffset() : 0;
        List<NextEditCandidate> sorted = new java.util.ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble(NextEditCandidate::score).reversed()
                        .thenComparingInt(candidate -> Math.abs(candidate.startIndex() - anchor)));
        return sorted;
    }

    /**
     * 编辑建议项类
     * <p> 用于表示文本编辑过程中可能的替换建议, 包含替换起始位置, 结束位置, 替换内容以及匹配得分.
     * <p> 该类为内部静态类, 通常用于拼写纠正或自动补全功能中, 表示一个具体的修改建议.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.05
     * @since 1.0.0
     */
    private static final class NextEditSuggestionItem {
        /** 起始索引位置, 表示文本编辑建议的起始字符位置. */
        private int startIndex;
        /** 结束索引, 表示文本编辑建议的结束字符位置 */
        private int endIndex;
        /**
         * 替换文本内容
         * <p> 表示该编辑建议项中用于替换原文本的具体字符串内容
         */
        private final String replacement;
        /** 编辑建议的评分, 用于排序或筛选建议项 */
        private final double score;

        /**
         * 构造一个编辑建议项对象
         * <p> 用于表示一个可能的文本编辑建议, 包含起始位置, 结束位置, 替换文本和匹配得分
         *
         * @param startIndex  起始索引, 表示建议文本在原文本中的起始位置
         * @param endIndex    结束索引, 表示建议文本在原文本中的结束位置
         * @param replacement 替换文本, 表示建议的替换内容, 不能为空
         * @param score       匹配得分, 表示该建议与当前输入的匹配程度
         */
        private NextEditSuggestionItem(int startIndex, int endIndex, @NotNull String replacement, double score) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.replacement = replacement;
            this.score = score;
        }

        /**
         * 调整起始和结束索引的偏移量
         * <p> 将当前项的起始索引和结束索引同时增加指定的偏移量 delta
         * <p> 常用于在文本编辑或字符串替换操作中同步更新索引位置
         *
         * @param delta 偏移量, 可以是正数或负数
         */
        private void shift(int delta) {
            startIndex += delta;
            endIndex += delta;
        }
    }
}
