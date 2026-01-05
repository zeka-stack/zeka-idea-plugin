package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.openapi.diagnostic.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Description  : 统计数据显示.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public class StatisticsUploader {

    /** 应用日志记录器, 用于统计上报过程中的日志输出 */
    private static final Logger LOG = Logger.getInstance(StatisticsUploader.class);
    /** 统计数据上报的 API 地址 */
    private static final String UPLOAD_URL = "https://api.intellai.com/v1/statistics/upload";

    /** 用户统计设置配置 */
    private final StatisticsSettings settings;
    /** 数据读取器, 用于读取统计文件中的记录数据 */
    private final StatisticsDataReader reader;
    /** 统计数据存储目录 */
    private final Path statisticsDir;
    /** JSON 序列化与反序列化工具, 用于将对象转换为 JSON 字符串及从 JSON 解析对象 */
    private final ObjectMapper objectMapper;

    /** 已上报的记录索引文件, 用于跟踪每个数据文件中已上传的记录数量 */
    private static final String UPLOADED_INDEX_FILE = "uploaded.idx";

    /** 上传状态追踪, 用于记录每个数据批次的重试次数 */
    private final Map<String, AtomicInteger> uploadRetryCount = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param settings      设置对象
     * @param reader        数据读取器
     * @param statisticsDir 统计数据目录
     */
    public StatisticsUploader(StatisticsSettings settings, StatisticsDataReader reader, Path statisticsDir) {
        this.settings = settings;
        this.reader = reader;
        this.statisticsDir = statisticsDir;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * 执行上报
     *
     * @return 是否成功
     */
    public boolean upload() {
        if (!settings.isEnableStatistics()) {
            return true;
        }

        try {
            // 获取所有数据文件
            File[] dataFiles = statisticsDir.toFile().listFiles((dir, name) -> name.endsWith(".data"));
            if (dataFiles == null) {
                return true;
            }

            for (File dataFile : dataFiles) {
                uploadFile(dataFile);
            }

            return true;
        } catch (Exception e) {
            LOG.error("Failed to upload statistics", e);
            return false;
        }
    }

    /**
     * 上报单个文件
     *
     * @param dataFile 数据文件
     */
    private void uploadFile(File dataFile) {
        try {
            // 读取未上报的记录
            List<StatisticsRecord> records = getUnreportedRecords(dataFile);
            if (records.isEmpty()) {
                return;
            }

            // 批量上报
            List<List<StatisticsRecord>> batches = partition(records, 100);
            for (List<StatisticsRecord> batch : batches) {
                boolean success = uploadBatch(batch);
                if (success) {
                    markAsUploaded(dataFile, batch.size());
                } else {
                    break; // 如果失败，停止上报，等待重试
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to upload file: " + dataFile.getName(), e);
        }
    }

    /**
     * 获取未上报的记录
     *
     * @param dataFile 数据文件
     * @return 未上报的记录列表
     */
    private List<StatisticsRecord> getUnreportedRecords(File dataFile) throws IOException {
        int uploadedCount = getUploadedCount(dataFile);
        List<StatisticsRecord> allRecords = reader.readAllRecords(dataFile);
        if (uploadedCount >= allRecords.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(allRecords.subList(uploadedCount, allRecords.size()));
    }

    /**
     * 批量上报
     *
     * @param records 记录列表
     * @return 是否成功
     */
    private boolean uploadBatch(List<StatisticsRecord> records) {
        // 检查重试次数
        String batchKey = generateBatchKey(records);
        int retryCount = uploadRetryCount.getOrDefault(batchKey, new AtomicInteger(0)).get();

        if (retryCount >= 3) {
            LOG.warn("Batch exceeded max retry count, skipping: " + batchKey);
            return true; // 跳过，继续上报其他批次
        }

        try {
            UploadRequest request = buildRequest(records);
            String jsonBody = objectMapper.writeValueAsString(request);

            String response = doPost(jsonBody);
            UploadResponse uploadResponse = objectMapper.readValue(response, UploadResponse.class);

            if (uploadResponse.getCode() == 0) {
                uploadRetryCount.remove(batchKey);
                return true;
            } else {
                handleError(uploadResponse, batchKey);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Failed to upload batch", e);
            uploadRetryCount.computeIfAbsent(batchKey, k -> new AtomicInteger(0)).incrementAndGet();
            return false;
        }
    }

    /**
     * 构建上报请求
     *
     * @param records 记录列表
     * @return 上报请求
     */
    private UploadRequest buildRequest(List<StatisticsRecord> records) {
        UploadRequest request = new UploadRequest();
        request.setDeviceId(settings.getDeviceId());
        request.setClientTimestamp(System.currentTimeMillis());

        List<UploadItem> items = new ArrayList<>();
        for (StatisticsRecord record : records) {
            UploadItem item = new UploadItem();
            item.setProjectName(record.getProjectName());
            item.setPluginId(record.getPluginId());
            item.setEventType(record.getEventType());
            item.setProvider(record.getProvider());
            item.setModel(record.getModel());
            item.setTokenCount(record.getTokenCount());
            item.setCreatedAt(record.getCreatedAt());
            items.add(item);
        }
        request.setItems(items);

        return request;
    }

    /**
     * 发送 HTTP POST 请求
     *
     * @param jsonBody 请求体
     * @return 响应内容
     * @throws IOException 如果发生 I/O 错误
     */
    private String doPost(String jsonBody) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(UPLOAD_URL);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("X-Client-Version", "1.0.0");
            post.setHeader("X-Client-Platform", "idea");

            post.setEntity(new StringEntity(jsonBody));

            try (CloseableHttpResponse response = client.execute(post)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    /**
     * 处理错误
     *
     * @param response 响应
     * @param batchKey 批次 key
     */
    private void handleError(UploadResponse response, String batchKey) {
        switch (response.getCode()) {
            case 429: // 请求过频，延迟重试
                try {
                    Thread.sleep(5 * 60 * 1000); // 5 分钟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;
            case 50001: // 服务器内部错误
            case 50002: // 数据库异常
                try {
                    Thread.sleep(60 * 1000); // 1 分钟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;
            default:
                uploadRetryCount.computeIfAbsent(batchKey, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    /**
     * 生成分批 key
     *
     * @param records 记录列表
     * @return key
     */
    private String generateBatchKey(List<StatisticsRecord> records) {
        if (records.isEmpty()) {
            return "";
        }
        StatisticsRecord first = records.get(0);
        return first.getProjectName() + "_" + first.getCreatedAt();
    }

    /**
     * 标记为已上报
     *
     * @param dataFile 数据文件
     * @param count    已上报的数量
     */
    private void markAsUploaded(File dataFile, int count) {
        try {
            Path indexFile = new File(statisticsDir.toFile(), dataFile.getName() + ".idx").toPath();
            Files.write(indexFile, (count + "\n").getBytes(), java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Failed to mark as uploaded", e);
        }
    }

    /**
     * 获取已上报数量
     *
     * @param dataFile 数据文件
     * @return 已上报数量
     */
    private int getUploadedCount(File dataFile) {
        try {
            Path indexFile = new File(statisticsDir.toFile(), dataFile.getName() + ".idx").toPath();
            if (!Files.exists(indexFile)) {
                return 0;
            }
            List<String> lines = Files.readAllLines(indexFile);
            return lines.stream().mapToInt(Integer::parseInt).sum();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 分割列表为多个子列表
     *
     * @param list 原始列表
     * @param size 每个子列表的最大元素数量
     * @return 分割后的子列表集合
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    // ==================== 内部类 ====================

    /** 上报请求类, 包含设备 ID, 客户端时间戳和上传项列表. */
    private static class UploadRequest {
        /** 设备唯一标识符 */
        private String deviceId;
        /**
         * 客户端时间戳
         * <p> 表示上传请求发起时的时间, 单位为毫秒 </p>
         */
        private long clientTimestamp;
        /**
         * 上报请求中的项目列表
         * <p> 包含需要上传的各项数据
         *
         * @see UploadItem
         */
        private List<UploadItem> items;

        /**
         * 获取设备 ID
         * <p> 返回当前对象的设备 ID
         *
         * @return 设备 ID
         */
        public String getDeviceId() {
            return deviceId;
        }

        /**
         * 设置设备 ID
         * <p> 为当前请求对象设置发送数据的设备唯一标识
         *
         * @param deviceId 设备 ID, 不能为 null 或空字符串
         */
        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        /**
         * 获取客户端时间戳
         * <p> 返回当前上传请求的客户端时间戳
         *
         * @return 客户端时间戳
         */
        public long getClientTimestamp() {
            return clientTimestamp;
        }

        /**
         * 设置客户端时间戳
         * <p> 将上报请求中的客户端时间戳设置为指定的值
         *
         * @param clientTimestamp 客户端时间戳
         */
        public void setClientTimestamp(long clientTimestamp) {
            this.clientTimestamp = clientTimestamp;
        }

        /**
         * 获取上传请求中的项目列表
         * <p> 返回当前上传请求中包含的所有项目列表
         *
         * @return 上传项目的列表
         */
        public List<UploadItem> getItems() {
            return items;
        }

        /**
         * 设置上传项列表
         * <p> 将指定的上传项集合赋值给当前对象的 items 字段
         *
         * @param items 上传项列表, 不能为 null
         */
        public void setItems(List<UploadItem> items) {
            this.items = items;
        }
    }

    /** 上报项 */
    private static class UploadItem {
        /** 项目名称 */
        private String projectName;
        /** 插件唯一标识符 */
        private String pluginId;
        /** 事件类型 */
        private String eventType;
        /**
         * 提供者名称
         * <p>
         * 用于标识上报项的提供者.
         */
        private String provider;
        /** 模型名称 */
        private String model;
        /**
         * 令牌计数
         * <p> 表示该上报项所涉及的令牌数量, 通常用于统计模型使用情况
         */
        private long tokenCount;
        /** 事件创建时间, 单位为毫秒 */
        private long createdAt;

        /**
         * 获取项目的名称
         * <p> 返回当前上报项的项目名称
         *
         * @return 项目的名称
         */
        public String getProjectName() {
            return projectName;
        }

        /**
         * 设置项目名称
         * <p> 将上传项的项目名称设置为指定的值
         *
         * @param projectName 要设置的项目名称
         */
        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        /**
         * 获取插件 ID
         * <p> 返回当前上传项的插件 ID, 该字段用于标识使用的插件信息.
         *
         * @return 插件 ID, 若未设置则返回空字符串
         */
        public String getPluginId() {
            return pluginId;
        }

        /**
         * 设置插件 ID
         * <p> 为当前上报项设置对应的插件标识符
         *
         * @param pluginId 插件 ID, 用于标识数据来源的插件
         */
        public void setPluginId(String pluginId) {
            this.pluginId = pluginId;
        }

        /**
         * 获取事件类型
         * <p> 返回当前上报项的事件类型名称
         *
         * @return 事件类型字符串
         */
        public String getEventType() {
            return eventType;
        }

        /**
         * 设置事件类型
         *
         * @param eventType 事件类型的字符串值
         */
        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        /**
         * 获取服务提供商
         * <p> 返回当前上报项中记录的服务提供商名称
         *
         * @return 服务提供商名称
         */
        public String getProvider() {
            return provider;
        }

        /**
         * 设置提供者信息
         * <p> 用于设置当前上报项的提供者标识, 该字段表示生成事件的来源或服务提供方.
         *
         * @param provider 提供者标识, 不能为空字符串
         */
        public void setProvider(String provider) {
            this.provider = provider;
        }

        /**
         * 获取模型名称
         * <p> 返回该对象关联的模型名称.
         *
         * @return 模型名称, 若未设置则返回 null
         */
        public String getModel() {
            return model;
        }

        /**
         * 设置模型名称
         * <p> 将上传项的模型名称设置为指定的值
         *
         * @param model 模型名称
         */
        public void setModel(String model) {
            this.model = model;
        }

        /**
         * 获取令牌计数
         * <p> 返回当前上传项的令牌计数.
         *
         * @return 令牌计数
         */
        public long getTokenCount() {
            return tokenCount;
        }

        /**
         * 设置上报项的 token 数量
         * <p> 此方法用于更新上报项中的 token 数量
         *
         * @param tokenCount 新的 token 数量
         */
        public void setTokenCount(long tokenCount) {
            this.tokenCount = tokenCount;
        }

        /**
         * 获取创建时间
         * <p> 返回当前对象的创建时间, 以毫秒为单位的时间戳
         *
         * @return 创建时间的时间戳
         */
        public long getCreatedAt() {
            return createdAt;
        }

        /**
         * 设置创建时间
         * <p> 用于记录该上报项的创建时间戳
         *
         * @param createdAt 创建时间, 单位为毫秒
         */
        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }
    }

    /** 上报响应 */
    private static class UploadResponse {
        /** 上报响应的状态码, 用于标识请求处理结果 */
        private int code;
        /** 上报响应的返回消息 */
        private String message;
        /** 响应数据信息 */
        private ResponseData data;

        /**
         * 获取上报响应的响应码
         * <p> 返回当前对象中的响应码
         *
         * @return 响应码
         */
        public int getCode() {
            return code;
        }

        /**
         * 设置上报响应的代码状态
         * <p> 用于设置上传响应对象中的状态码, 通常用于标识请求处理的结果
         *
         * @param code 状态码, 表示请求处理的状态
         */
        public void setCode(int code) {
            this.code = code;
        }

        /**
         * 获取消息内容
         * <p> 返回当前对象的消息字段值
         *
         * @return 消息内容字符串, 可能为 null
         */
        public String getMessage() {
            return message;
        }

        /**
         * 设置上报响应的消息
         * <p> 此方法用于设置上报响应对象中的消息信息
         *
         * @param message 要设置的消息内容
         */
        public void setMessage(String message) {
            this.message = message;
        }

        /**
         * 获取响应数据
         *
         * @return 包含已接收数量和失败项 ID 列表的响应数据对象
         */
        public ResponseData getData() {
            return data;
        }

        /**
         * 设置响应数据
         * <p> 将指定的响应数据对象设置到当前实例中
         *
         * @param data 响应数据对象, 不能为 null
         */
        public void setData(ResponseData data) {
            this.data = data;
        }

        /**
         * 响应数据封装类
         * <p> 用于封装接口响应结果, 包含已接收数据条数及失败项列表, 适用于批量操作结果的返回场景
         * <p> 主要用途: 在批量处理请求后, 返回处理结果统计信息, 便于前端或调用方判断处理状态
         * <p> 使用示例:
         * <pre>{@code
         * ResponseData responseData = new ResponseData();
         * responseData.setReceivedCount(100);
         * responseData.setFailedItems(Arrays.asList(1, 3, 5));
         * }</pre>
         *
         * @author dong4j
         * @version 1.0.0
         * @email "mailto:dong4j@gmail.com"
         * @date 2026.01.05
         * @since 1.0.0
         */
        static class ResponseData {
            /**
             * 接收到的数据项数量
             * <p> 表示成功接收到的数据项总数
             */
            private int receivedCount;
            /**
             * 失败项列表
             * <p> 存储在请求或操作中失败的项目索引
             *
             */
            private List<Integer> failedItems;

            /**
             * 获取已接收的消息数量
             * <p> 返回 ResponseData 对象中记录的已接收消息总数
             *
             * @return 已接收的消息数量
             */
            public int getReceivedCount() {
                return receivedCount;
            }

            /**
             * 设置接收到的项目总数
             * <p> 该方法用于更新当前对象中记录的已接收项目数量
             *
             * @param receivedCount 已接收的项目数量, 必须为非负整数
             */
            public void setReceivedCount(int receivedCount) {
                this.receivedCount = receivedCount;
            }

            /**
             * 获取失败项列表
             * <p> 返回包含所有失败项的列表
             *
             * @return 失败项的列表, 如果没有任何失败项则返回空列表
             */
            public List<Integer> getFailedItems() {
                return failedItems;
            }

            /**
             * 设置处理失败的项目 ID 列表
             * <p> 用于记录在操作过程中未能成功处理的项目标识符
             *
             * @param failedItems 失败项目的 ID 列表, 不能为 null
             */
            public void setFailedItems(List<Integer> failedItems) {
                this.failedItems = failedItems;
            }
        }
    }
}
