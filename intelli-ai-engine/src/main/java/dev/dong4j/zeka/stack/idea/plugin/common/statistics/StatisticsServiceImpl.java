package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * 统计服务实现类, 用于处理统计事件的收集, 存储和上传.
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {
    /** 写入间隔:5 分钟 (以毫秒为单位) */
    private static final long WRITE_INTERVAL_MS = 5 * 60 * 1000;
    /** 上报间隔:30 分钟 */
    private static final long UPLOAD_INTERVAL_MS = 10 * 60 * 1000;

    /** 统计服务配置信息 */
    private final StatisticsSettings settings;
    /** 用于存储统计事件的阻塞队列, 用于异步缓冲事件数据 */
    private final BlockingQueue<StatisticsEvent> eventQueue;
    /** 用于调度定时任务的线程池, 支持写入和上传操作的周期性执行 */
    private final ScheduledExecutorService scheduler;
    /** 统计数据写入器, 用于将统计数据写入文件 */
    private final StatisticsDataWriter writer;
    /** 用于读取统计记录数据的读取器 */
    private final StatisticsDataReader reader;
    /** 负责统计数据上传的组件, 用于将本地记录上传到远程服务器 */
    private final StatisticsUploader uploader;

    /** 服务运行状态标志, 用于控制服务的启动与停止 */
    private volatile boolean running = false;
    /** 写入任务的调度句柄, 用于控制定时写入操作的生命周期 */
    private ScheduledFuture<?> writeTask;
    /** 上载任务 */
    private ScheduledFuture<?> uploadTask;

    /**
     * 构造统计服务实现类实例
     * <p> 初始化统计服务所需的核心组件, 包括配置, 事件队列, 线程池, 数据读写器和上传器.
     * 该构造函数用于创建一个完整的统计服务运行环境.
     *
     * @since 1.4.0
     */
    public StatisticsServiceImpl() {
        this.settings = StatisticsSettings.getInstance();
        this.eventQueue = new LinkedBlockingQueue<>(10000);
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Statistics");
            t.setDaemon(true);
            return t;
        });

        File statsDir = settings.getStatisticsDirectory();
        this.writer = new StatisticsDataWriter(statsDir);
        this.reader = new StatisticsDataReader(statsDir);
        this.uploader = new StatisticsUploader(settings, reader, statsDir.toPath());
    }

    /**
     * 报告单个统计事件
     * <p> 如果统计功能已启用, 则将事件添加到事件队列中; 如果队列已满, 则记录警告并丢弃该事件
     *
     * @param event 要报告的统计事件, 不能为 null
     */
    @Override
    public void report(StatisticsEvent event) {
        if (!settings.isEnableStatistics()) {
            return;
        }

        if (!eventQueue.offer(event)) {
            log.debug("Statistics queue is full, dropping event: " + event);
        }
    }

    /**
     * 批量报告统计事件
     * <p> 在批量模式下, 逐个调用单个事件报告方法来处理每个统计事件
     * <p> 如果统计功能未启用, 则直接返回, 不会处理任何事件
     *
     * @param events 要报告的统计事件列表, 不能为 null
     */
    @Override
    public void reportBatch(List<StatisticsEvent> events) {
        if (!settings.isEnableStatistics()) {
            return;
        }

        for (StatisticsEvent event : events) {
            report(event);
        }
    }

    /**
     * 获取统计快照
     * <p> 根据指定日期或所有日期数据生成统计快照. 若日期为空或为空字符串, 则获取所有数据文件的快照; 否则仅获取指定日期的数据快照.
     * <p> 示例:
     * <pre>{@code
     * StatisticsSnapshot snapshotAll = snapshot(null); // 获取所有日期的快照
     * StatisticsSnapshot snapshotToday = snapshot("2025-04-01"); // 获取指定日期的快照
     * }</pre>
     *
     * @param date 日期字符串, 若为 null 或空字符串则获取所有日期数据, 否则仅获取指定日期数据
     * @return 统计快照对象, 包含所有或指定日期的统计记录
     */
    @Override
    public StatisticsSnapshot snapshot(String date) {
        StatisticsSnapshot snapshot = new StatisticsSnapshot();

        try {
            if (date == null || date.isEmpty()) {
                // 获取所有日期的数据
                List<String> dates = reader.getAllDataFileDates();
                for (String d : dates) {
                    addFileToSnapshot(snapshot, d);
                }
            } else {
                addFileToSnapshot(snapshot, date);
            }
        } catch (Exception e) {
            log.debug("Failed to get snapshot for date: " + date, e);
        }

        return snapshot;
    }

    /**
     * 将文件数据添加到快照中
     *
     * @param snapshot 快照对象, 用于存储统计数据
     * @param dateStr  日期字符串, 表示要读取的数据文件对应的日期
     */
    private void addFileToSnapshot(StatisticsSnapshot snapshot, String dateStr) {
        File dataFile = writer.getDataFile(dateStr);
        if (dataFile.exists()) {
            try {
                List<StatisticsRecord> records = reader.readAllRecords(dataFile);
                for (StatisticsRecord record : records) {
                    snapshot.addRecord(record);
                }
            } catch (Exception e) {
                log.debug("Failed to read data file for date: " + dateStr, e);
            }
        }
    }

    /**
     * 获取聚合的统计信息
     * <p> 通过获取全部统计数据快照并转换为 Map 格式返回
     *
     * @return 包含所有聚合统计信息的 Map, 键为字符串类型, 值为对应统计项的数据
     */
    @Override
    public Map<String, Object> getAggregatedStats() {
        StatisticsSnapshot snapshot = snapshot(null);
        return snapshot.toMap();
    }

    /**
     * 获取统计服务是否启用的状态
     * <p> 检查配置设置以确定统计服务是否启用
     *
     * @return 如果统计服务已启用, 则返回 true; 否则返回 false
     */
    @Override
    public boolean isEnabled() {
        return settings.isEnableStatistics();
    }

    /**
     * 刷新统计数据到文件
     * <p> 将当前事件队列中的所有统计数据刷新到文件中, 并清空队列
     * <p> 如果队列为空, 则不会执行任何操作
     * <p>
     * 该方法会尝试将所有的统计数据转换为记录并写入文件. 如果写入失败, 会重新将未成功写入的数据放回队列中.
     *
     * @see #flushToFile()
     */
    @Override
    public void flush() {
        flushToFile();
    }

    /**
     * 立即执行统计信息的上传操作
     * <p> 该方法会通过 ApplicationManager 调度到 UI 线程, 并创建一个后台任务来执行上传过程.
     * <p> 上传过程中会在进度指示器上显示 "Uploading statistics..." 提示信息.
     *
     * @since 1.4.0
     */
    @Override
    public void uploadNow() {
        ApplicationManager.getApplication().invokeLater(() -> {
            new Task.Backgroundable(null, "Uploading statistics...") {
                /**
                 * 执行后台任务, 上传统计数据
                 * <p> 设置进度指示器的文本为 "Uploading statistics..." 并调用上传器执行上传操作
                 *
                 * @param indicator 进度指示器, 用于显示任务状态信息, 不能为 null
                 */
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText("Uploading statistics...");
                    uploader.upload();
                }
            }.queue();
        });
    }

    /** 启动统计服务, 初始化定时写入和上报任务 */
    @Override
    public void start() {
        if (running) {
            return;
        }

        if (!settings.isEnableStatistics()) {
            return;
        }

        running = true;

        // 定时写入任务
        writeTask = scheduler.scheduleAtFixedRate(
            this::flushToFile,
            WRITE_INTERVAL_MS,
            WRITE_INTERVAL_MS,
            TimeUnit.MILLISECONDS
                                                 );

        // 定时上报任务
        uploadTask = scheduler.scheduleAtFixedRate(
            uploader::upload,
            UPLOAD_INTERVAL_MS,
            UPLOAD_INTERVAL_MS,
            TimeUnit.MILLISECONDS
                                                  );

        log.debug("Statistics service started");
    }

    /** 停止服务 */
    @Override
    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        // 取消定时任务
        if (writeTask != null) {
            writeTask.cancel(false);
        }
        if (uploadTask != null) {
            uploadTask.cancel(false);
        }

        // 最后一次写入
        flushToFile();

        log.debug("Statistics service stopped");
    }

    /**
     * 刷新统计事件到文件.
     * <p>
     * 从事件队列中提取所有统计事件, 并将其转换为记录后写入文件. 如果写入失败, 则将事件重新放回队列.
     */
    private void flushToFile() {
        if (eventQueue.isEmpty()) {
            return;
        }

        List<StatisticsEvent> events = new ArrayList<>();
        eventQueue.drainTo(events);

        if (events.isEmpty()) {
            return;
        }

        try {
            List<StatisticsRecord> records = new ArrayList<>();
            for (StatisticsEvent event : events) {
                records.add(event.toRecord());
            }
            writer.writeRecords(records);
            log.debug("Flushed " + records.size() + " records to file");
        } catch (Exception e) {
            log.debug("Failed to flush statistics to file", e);
            // 如果写入失败，将事件重新放回队列
            for (StatisticsEvent event : events) {
                eventQueue.offer(event);
            }
        }
    }
}
