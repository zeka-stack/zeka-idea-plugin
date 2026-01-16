package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 提交选择元数据
 * <p>
 * 用于在生成 commit message 的结构化上下文中补充“选择信息”（例如来自 Git Log 的单条/多条提交）。
 */
record CommitSelectionMeta(@NotNull String type,
                           @NotNull List<String> hashes,
                           @NotNull List<String> titles) {
}

