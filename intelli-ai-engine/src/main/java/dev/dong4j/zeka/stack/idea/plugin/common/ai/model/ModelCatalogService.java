package dev.dong4j.zeka.stack.idea.plugin.common.ai.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.kit.SiteContents;
import dev.dong4j.zeka.stack.idea.plugin.kit.StorageUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 远程推荐模型目录服务
 * <p>
 * 数据源：{@code GET /api/plugin/v1/models} 或 {@code GET /api/plugin/v1/models/{providerId}}。
 * 优先级由调用方组合：live 缓存（刷新模型）→ 本服务 → 枚举 seed。
 * <p>
 * 本类方法：
 * <ul>
 *   <li>{@link #peek}：仅读内存/磁盘，不访问网络（可在 EDT）</li>
 *   <li>{@link #refreshAsync}/{@link #prefetchAllAsync}：后台拉取并写缓存</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class ModelCatalogService {

    private static final long TTL_MS = 24L * 60L * 60L * 1000L;
    private static final int REQUEST_TIMEOUT_MS = 5000;
    private static final String CACHE_FILE = "model-catalog.json";
    private static final Gson GSON = new Gson();

    private static final ConcurrentHashMap<String, CacheHolder> MEMORY = new ConcurrentHashMap<>();
    private static final AtomicBoolean PREFETCHING = new AtomicBoolean(false);
    private static final Object DISK_LOCK = new Object();

    private ModelCatalogService() {
    }

    /**
     * 同步查看缓存（内存 → 磁盘），不发起网络请求
     *
     * @param providerId 插件 providerId
     * @return 目录；无缓存返回 {@link ModelCatalogEntry#EMPTY}
     */
    @NotNull
    public static ModelCatalogEntry peek(@NotNull String providerId) {
        String id = normalize(providerId);
        CacheHolder mem = MEMORY.get(id);
        if (mem != null && !mem.entry.isEmpty()) {
            return mem.entry;
        }
        loadDiskIntoMemory();
        mem = MEMORY.get(id);
        return mem != null ? mem.entry : ModelCatalogEntry.EMPTY;
    }

    /**
     * 缓存是否过期（无缓存视为过期）
     */
    public static boolean isStale(@NotNull String providerId) {
        String id = normalize(providerId);
        CacheHolder mem = MEMORY.get(id);
        if (mem == null) {
            loadDiskIntoMemory();
            mem = MEMORY.get(id);
        }
        if (mem == null || mem.entry.isEmpty()) {
            return true;
        }
        return System.currentTimeMillis() - mem.fetchedAt > TTL_MS;
    }

    /**
     * 后台预取全量目录（打开设置时调用一次即可）
     *
     * @param onDone 完成回调（任意线程）；可为 null
     */
    public static void prefetchAllAsync(@Nullable Runnable onDone) {
        if (!PREFETCHING.compareAndSet(false, true)) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Map<String, ModelCatalogEntry> all = fetchAllFromRemote();
                if (all != null && !all.isEmpty()) {
                    long now = System.currentTimeMillis();
                    for (Map.Entry<String, ModelCatalogEntry> e : all.entrySet()) {
                        putMemory(e.getKey(), e.getValue(), now);
                    }
                    saveDisk(now);
                }
            } catch (Exception e) {
                log.debug("prefetch model catalog failed", e);
            } finally {
                PREFETCHING.set(false);
                if (onDone != null) {
                    onDone.run();
                }
            }
        });
    }

    /**
     * 后台刷新单个 provider；优先用已缓存全量，否则请求 {@code /{providerId}}
     *
     * @param providerId providerId
     * @param onUpdated  成功拿到非空目录时回调（后台线程）
     */
    public static void refreshAsync(@NotNull String providerId, @Nullable Consumer<ModelCatalogEntry> onUpdated) {
        String id = normalize(providerId);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 若全量过期或缺失，先尝试全量
                if (isStale(id) || MEMORY.isEmpty()) {
                    Map<String, ModelCatalogEntry> all = fetchAllFromRemote();
                    if (all != null && !all.isEmpty()) {
                        long now = System.currentTimeMillis();
                        for (Map.Entry<String, ModelCatalogEntry> e : all.entrySet()) {
                            putMemory(e.getKey(), e.getValue(), now);
                        }
                        saveDisk(now);
                    }
                }
                ModelCatalogEntry entry = peek(id);
                if (entry.isEmpty()) {
                    entry = fetchOneFromRemote(id);
                    if (!entry.isEmpty()) {
                        long now = System.currentTimeMillis();
                        putMemory(id, entry, now);
                        saveDisk(now);
                    }
                }
                if (onUpdated != null && !entry.isEmpty()) {
                    onUpdated.accept(entry);
                }
            } catch (Exception e) {
                log.debug("refresh model catalog failed: {}", id, e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // remote
    // -------------------------------------------------------------------------

    @Nullable
    private static Map<String, ModelCatalogEntry> fetchAllFromRemote() {
        try {
            String body = HttpRequests.request(SiteContents.MODEL_API_BASE_URL)
                .productNameAsUserAgent()
                .readTimeout(REQUEST_TIMEOUT_MS)
                .readString();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!isSuccess(root) || !root.has("data")) {
                return null;
            }
            JsonElement dataEl = root.get("data");
            // 全量：data 为 map；单条兼容不走这里
            if (!dataEl.isJsonObject()) {
                return null;
            }
            JsonObject data = dataEl.getAsJsonObject();
            // 若是单条 ModelResponse（含 models 数组），不是全量
            if (data.has("models") && data.get("models").isJsonArray()) {
                return null;
            }
            Map<String, ModelCatalogEntry> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : data.entrySet()) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                ModelCatalogEntry entry = parseProviderObject(e.getValue().getAsJsonObject());
                if (!entry.isEmpty()) {
                    result.put(normalize(e.getKey()), entry);
                }
            }
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            log.debug("fetch all model catalog failed", e);
            return null;
        }
    }

    @NotNull
    private static ModelCatalogEntry fetchOneFromRemote(@NotNull String providerId) {
        try {
            String url = SiteContents.MODEL_API_BASE_URL + "/" + providerId;
            String body = HttpRequests.request(url)
                .productNameAsUserAgent()
                .readTimeout(REQUEST_TIMEOUT_MS)
                .readString();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!isSuccess(root) || !root.has("data") || !root.get("data").isJsonObject()) {
                return ModelCatalogEntry.EMPTY;
            }
            return parseProviderObject(root.getAsJsonObject("data"));
        } catch (Exception e) {
            log.debug("fetch one model catalog failed: {}", providerId, e);
            return ModelCatalogEntry.EMPTY;
        }
    }

    private static boolean isSuccess(@NotNull JsonObject root) {
        return root.has("success") && root.get("success").isJsonPrimitive() && root.get("success").getAsBoolean();
    }

    @NotNull
    private static ModelCatalogEntry parseProviderObject(@NotNull JsonObject obj) {
        String defaultModel = obj.has("defaultModel") && obj.get("defaultModel").isJsonPrimitive()
                              ? obj.get("defaultModel").getAsString()
                              : null;
        List<String> models = new ArrayList<>();
        if (obj.has("models") && obj.get("models").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("models")) {
                if (el.isJsonPrimitive()) {
                    String m = el.getAsString();
                    if (m != null && !m.isBlank()) {
                        models.add(m.trim());
                    }
                }
            }
        }
        return ModelCatalogEntry.of(defaultModel, models);
    }

    // -------------------------------------------------------------------------
    // memory / disk
    // -------------------------------------------------------------------------

    private static void putMemory(@NotNull String providerId, @NotNull ModelCatalogEntry entry, long fetchedAt) {
        MEMORY.put(normalize(providerId), new CacheHolder(fetchedAt, entry));
    }

    private static void loadDiskIntoMemory() {
        synchronized (DISK_LOCK) {
            Path path = cachePath();
            if (path == null || !Files.exists(path)) {
                return;
            }
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                DiskCache disk = GSON.fromJson(json, DiskCache.class);
                if (disk == null || disk.providers == null) {
                    return;
                }
                long fetchedAt = disk.fetchedAt > 0 ? disk.fetchedAt : System.currentTimeMillis();
                for (Map.Entry<String, CachedProvider> e : disk.providers.entrySet()) {
                    if (e.getValue() == null) {
                        continue;
                    }
                    ModelCatalogEntry entry = ModelCatalogEntry.of(e.getValue().defaultModel, e.getValue().models);
                    if (!entry.isEmpty()) {
                        MEMORY.putIfAbsent(normalize(e.getKey()), new CacheHolder(fetchedAt, entry));
                    }
                }
            } catch (Exception e) {
                log.debug("load model catalog disk cache failed", e);
            }
        }
    }

    private static void saveDisk(long fetchedAt) {
        synchronized (DISK_LOCK) {
            Path path = cachePath();
            if (path == null) {
                return;
            }
            try {
                DiskCache disk = new DiskCache();
                disk.fetchedAt = fetchedAt;
                disk.providers = new LinkedHashMap<>();
                for (Map.Entry<String, CacheHolder> e : MEMORY.entrySet()) {
                    CachedProvider p = new CachedProvider();
                    p.defaultModel = e.getValue().entry.defaultModel();
                    p.models = new ArrayList<>(e.getValue().entry.models());
                    disk.providers.put(e.getKey(), p);
                }
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(disk), StandardCharsets.UTF_8,
                                  StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (Exception e) {
                log.debug("save model catalog disk cache failed", e);
            }
        }
    }

    @Nullable
    private static Path cachePath() {
        try {
            return StorageUtil.resolve(EngineContents.PLUGIN_SIMPLE_NAME, CACHE_FILE);
        } catch (Exception e) {
            return null;
        }
    }

    @NotNull
    private static String normalize(@NotNull String providerId) {
        return providerId.trim().toLowerCase(Locale.ROOT);
    }

    private record CacheHolder(long fetchedAt, @NotNull ModelCatalogEntry entry) {
    }

    @SuppressWarnings("unused")
    private static final class DiskCache {
        long fetchedAt;
        Map<String, CachedProvider> providers;
    }

    @SuppressWarnings("unused")
    private static final class CachedProvider {
        String defaultModel;
        List<String> models;
    }
}
