package dev.dong4j.zeka.stack.idea.plugin.terminal.history;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Terminal 历史记录项目级持久化状态
 */
@Service(Service.Level.PROJECT)
@State(
    name = "TerminalHistoryState",
    storages = @Storage("zeka.stack.terminal.history.xml")
)
public final class TerminalHistoryState implements PersistentStateComponent<TerminalHistoryState> {
    /** 最大历史记录条数, 超过时自动移除最早条目 */
    private static final int MAX_HISTORY_SIZE = 200;

    /**
     * 终端历史记录条目列表
     * <p> 存储用户与终端交互的完整历史记录, 包含时间戳, 提示和应答信息 </p>
     * <p> 列表大小受 MAX_HISTORY_SIZE 限制, 自动移除最早条目以保持固定大小 </p>
     */
    public List<HistoryEntry> entries = new ArrayList<>();

    /**
     * 获取当前项目中的 TerminalHistoryState 实例
     * <p> 通过传入的项目对象获取 TerminalHistoryState 的服务实例
     *
     * @param project 项目对象
     * @return TerminalHistoryState 实例
     */
    public static TerminalHistoryState getInstance(@NotNull Project project) {
        return project.getService(TerminalHistoryState.class);
    }

    /**
     * 获取当前状态的实例
     * <p> 返回当前对象本身作为状态实例
     *
     * @return 当前状态实例, 若未初始化则返回 null
     */
    @Override
    public @Nullable TerminalHistoryState getState() {
        return this;
    }

    /**
     * 载入并恢复持久化状态
     * <p> 将传入的 {@code TerminalHistoryState} 对象中的属性复制到当前实例, 从而重建持久化的状态信息.
     *
     * @param state 要复制的状态实例, 不能为空
     */
    @Override
    public void loadState(@NotNull TerminalHistoryState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 添加一条新的历史记录条目
     * <p> 如果提示词或回答为空, 则不添加.
     * 如果当前历史记录数量已达到最大限制, 则移除最早的一条记录后再添加新记录.
     *
     * @param timestamp 时间戳
     * @param prompt    用户输入的提示词
     * @param answer    终端返回的回答
     */
    public synchronized void add(@NotNull String timestamp, @NotNull String prompt, @NotNull String answer) {
        if (prompt.isBlank() || answer.isBlank()) {
            return;
        }
        if (entries.size() >= MAX_HISTORY_SIZE) {
            entries.remove(0);
        }
        HistoryEntry entry = new HistoryEntry();
        entry.timestamp = timestamp;
        entry.prompt = prompt;
        entry.answer = answer;
        entries.add(entry);
    }

    /**
     * 获取当前历史记录的快照副本
     * <p> 返回历史记录列表的不可变副本, 避免外部直接修改原始数据
     *
     * @return 历史记录条目的不可变副本列表
     */
    public synchronized @NotNull List<HistoryEntry> snapshot() {
        return new ArrayList<>(entries);
    }

    /**
     * 历史记录条目类
     * <p> 用于封装终端会话中每一次交互的历史记录, 包含时间戳, 用户输入提示和系统响应内容.
     * 该类作为 {@link TerminalHistoryState} 的内部嵌套类, 用于在持久化存储中保存单条历史记录数据.
     * <p> 支持序列化与反序列化, 适用于 IDE 或应用中终端历史记录的存储与恢复.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.02.10
     * @since 1.0.0
     */
    public static class HistoryEntry {
        /** 记录事件的时间戳 */
        public String timestamp = "";
        /** 当前提示内容 */
        public String prompt = "";
        /** 回答内容 */
        public String answer = "";
    }
}
