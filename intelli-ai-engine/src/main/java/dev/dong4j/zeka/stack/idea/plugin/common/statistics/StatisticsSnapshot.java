package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计快照类, 用于聚合和存储统计记录数据.
 *
 * @author dong4j
 * @version 1.4.0
 * @email dong4j@gmail.com
 * @date 2025.01.05
 */
public class StatisticsSnapshot {

    /** 记录列表 */
    private List<StatisticsRecord> records = new ArrayList<>();
    /** 总记录数 */
    private int totalCount;
    /** 按插件统计 */
    private Map<String, Long> byPlugin = new HashMap<>();
    /** 按事件类型统计的映射表, 键为事件类型名称, 值为对应事件数量. */
    private Map<String, Long> byEventType = new HashMap<>();
    /**
     * 按服务商统计的记录数映射
     * 键为服务商名称, 值为对应的服务商记录数量
     */
    private Map<String, Long> byProvider = new HashMap<>();
    /** 按项目统计 */
    private Map<String, Long> byProject = new HashMap<>();
    /** Token 消耗总计 */
    private long totalTokenCount;

    /**
     * 构造函数, 初始化统计快照对象
     * <p> 该构造函数用于创建一个空的 StatisticsSnapshot 实例, 所有统计信息初始为空.
     *
     */
    public StatisticsSnapshot() {
    }

    /**
     * 向统计快照中添加一条记录, 并更新相关统计信息
     * <p> 该方法将指定的统计记录加入到内部记录列表中, 同时根据记录内容自动更新各类聚合统计项, 包括插件, 事件类型, 服务商, 项目以及 Token 总消耗量.
     *
     * @param record 要添加的统计记录对象, 不能为 null
     */
    public void addRecord(StatisticsRecord record) {
        records.add(record);
        totalCount++;

        // 聚合统计
        byPlugin.merge(record.getPluginId(), 1L, Long::sum);
        byEventType.merge(record.getEventType(), 1L, Long::sum);
        if (record.getProvider() != null) {
            byProvider.merge(record.getProvider(), 1L, Long::sum);
        }
        if (record.getProjectName() != null) {
            byProject.merge(record.getProjectName(), 1L, Long::sum);
        }
        totalTokenCount += record.getTokenCount();
    }

    /**
     * 获取统计记录列表
     * <p> 返回当前统计快照中包含的所有统计记录
     *
     * @return 统计记录列表, 如果无记录则返回空列表
     */
    public List<StatisticsRecord> getRecords() {
        return records;
    }

    /**
     * 设置统计记录列表
     * <p> 将传入的统计记录列表赋值给当前对象的 records 字段.
     *
     * @param records 统计记录列表, 不能为 null
     */
    public void setRecords(List<StatisticsRecord> records) {
        this.records = records;
    }

    /**
     * 获取总记录数
     * <p> 返回统计快照中记录的总数
     *
     * @return 总记录数
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * 设置总记录数
     *
     * @param totalCount 总记录数, 必须为非负整数
     */
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * 获取按插件统计的结果
     * <p> 返回一个 Map, 键为插件 ID, 值为对应的记录数量
     *
     * @return 按插件统计的 Map
     */
    public Map<String, Long> getByPlugin() {
        return byPlugin;
    }

    /**
     * 设置按插件统计的数据
     * <p> 用于设置按插件分类的统计数据, 该数据通常由 {@link #addRecord(StatisticsRecord)} 方法自动更新 </p>
     *
     * @param byPlugin 按插件分类的统计信息, 键为插件 ID, 值为对应的记录数量
     */
    public void setByPlugin(Map<String, Long> byPlugin) {
        this.byPlugin = byPlugin;
    }

    /**
     * 获取按事件类型统计的映射表
     * <p> 返回一个 Map, 其中键为事件类型名称, 值为对应事件类型的记录数量
     *
     * @return 按事件类型统计的映射表
     */
    public Map<String, Long> getByEventType() {
        return byEventType;
    }

    /**
     * 设置按事件类型的统计数据
     * <p> 该方法用于更新按事件类型的统计数据映射.
     *
     * @param byEventType 事件类型的统计数据映射, 不能为 null
     */
    public void setByEventType(Map<String, Long> byEventType) {
        this.byEventType = byEventType;
    }

    /**
     * 获取按服务商统计的记录数映射
     *
     * @return 以服务商为键, 记录数为值的 Map, 可能包含 null 键 (如果服务商信息缺失则不统计)
     */
    public Map<String, Long> getByProvider() {
        return byProvider;
    }

    /**
     * 设置按服务商统计的数据
     * <p> 将指定的 Map 数据赋值给内部的 byProvider 字段, 用于记录各服务商的统计信息.
     *
     * @param byProvider 按服务商统计的数据, 键为服务商名称, 值为对应的统计数量
     */
    public void setByProvider(Map<String, Long> byProvider) {
        this.byProvider = byProvider;
    }

    /**
     * 获取按项目统计的映射
     * <p> 返回一个映射, 其中键为项目名称, 值为对应的记录数量
     *
     * @return 按项目统计的映射
     */
    public Map<String, Long> getByProject() {
        return byProject;
    }

    /**
     * 设置按项目统计的数据
     * <p> 将传入的项目统计信息设置到内部变量中, 用于后续数据处理或展示.
     *
     * @param byProject 按项目统计的信息, 键为项目名称, 值为对应的统计数量
     */
    public void setByProject(Map<String, Long> byProject) {
        this.byProject = byProject;
    }

    /**
     * 获取总 Token 消耗计数
     * <p> 返回统计快照中的总 Token 消耗数量
     *
     * @return 总 Token 消耗计数
     */
    public long getTotalTokenCount() {
        return totalTokenCount;
    }

    /**
     * 设置总的 Token 消耗数量
     * <p> 将指定的 Token 消耗数量赋值给内部变量, 用于统计总消耗量.
     *
     * @param totalTokenCount 总的 Token 消耗数量
     */
    public void setTotalTokenCount(long totalTokenCount) {
        this.totalTokenCount = totalTokenCount;
    }

    /**
     * 转换为 JSON 友好的 Map
     *
     * @return 包含统计信息的 Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("total", totalCount);
        map.put("byPlugin", byPlugin);
        map.put("byEventType", byEventType);
        map.put("byProvider", byProvider);
        map.put("byProject", byProject);
        map.put("totalTokenCount", totalTokenCount);
        map.put("records", records);
        return map;
    }
}
