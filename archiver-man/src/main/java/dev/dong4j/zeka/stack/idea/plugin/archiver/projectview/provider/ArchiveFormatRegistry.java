package dev.dong4j.zeka.stack.idea.plugin.archiver.projectview.provider;

import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一管理所有归档格式 provider，以便根据文件动态匹配。
 *
 * @author dong4j
 * @since 0.3.0
 */
public final class ArchiveFormatRegistry {

    private final List<ArchiveFormatProvider> providers = new ArrayList<>();

    private ArchiveFormatRegistry() {
        // 默认注册 Zip/Jar provider，后续可通过构造添加更多格式
        providers.add(new ZipArchiveFormatProvider());
    }

    public static ArchiveFormatRegistry getInstance() {
        return Holder.INSTANCE;
    }

    public void register(@NotNull ArchiveFormatProvider provider) {
        providers.add(provider);
    }

    public @NotNull List<ArchiveFormatProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    public @Nullable ArchiveFormatProvider findProvider(@NotNull VirtualFile file) {
        return providers.stream()
            .filter(provider -> provider.supports(file))
            .findFirst()
            .orElse(null);
    }

    private static final class Holder {
        private static final ArchiveFormatRegistry INSTANCE = new ArchiveFormatRegistry();
    }
}

