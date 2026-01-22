import React, {useEffect, useState} from 'react';
import {Activity, BarChart3, Calendar, Clock, Code2, Cpu, FolderKanban, Layers, PieChart, TrendingUp, Zap} from 'lucide-react';

const SAMPLE_DATA = {
    source: 'local-json',
    generatedAt: 1737264000000,
    records: [
        {
            pluginId: 'javadoc',
            eventType: 'javadoc_class',
            provider: 'qianwen',
            model: 'qwen2.5-coder-7b',
            tokenCount: 820,
            inputToken: 560,
            outputToken: 260,
            latencyMs: 1830,
            resultStatus: 'success',
            userAction: 'editor_context_menu',
            createdAt: 1737264000123,
            projectName: 'zeka-idea-plugin',
            deviceId: 'dev-1'
        },
        {
            pluginId: 'javadoc',
            eventType: 'javadoc_method',
            provider: 'qianwen',
            model: 'qwen2.5-coder-7b',
            tokenCount: 410,
            inputToken: 260,
            outputToken: 150,
            latencyMs: 980,
            resultStatus: 'success',
            userAction: 'editor_shortcut',
            createdAt: 1737264100456,
            projectName: 'zeka-idea-plugin',
            deviceId: 'dev-1'
        },
        {
            pluginId: 'changelog',
            eventType: 'changelog_commit_message',
            provider: 'openai',
            model: 'gpt-4o-mini',
            tokenCount: 560,
            inputToken: 360,
            outputToken: 200,
            latencyMs: 1420,
            resultStatus: 'success',
            userAction: 'commit_panel',
            createdAt: 1737264200789,
            projectName: 'zeka-idea-plugin',
            deviceId: 'dev-1'
        },
        {
            pluginId: 'changelog',
            eventType: 'changelog_release_log',
            provider: 'openai',
            model: 'gpt-4o-mini',
            tokenCount: 930,
            inputToken: 650,
            outputToken: 280,
            latencyMs: 2550,
            resultStatus: 'success',
            userAction: 'project_tree_context_menu_dir',
            createdAt: 1737264300999,
            projectName: 'zeka-idea-plugin',
            deviceId: 'dev-1'
        },
        {
            pluginId: 'javadoc',
            eventType: 'javadoc_field',
            provider: 'ollama',
            model: 'deepseek-coder',
            tokenCount: 230,
            inputToken: 140,
            outputToken: 90,
            latencyMs: 720,
            resultStatus: 'failed',
            userAction: 'editor_context_menu',
            createdAt: 1737264400111,
            projectName: 'zeka-idea-plugin',
            deviceId: 'dev-1'
        },
        {
            pluginId: 'changelog',
            eventType: 'changelog_daily_report',
            provider: 'openai',
            model: 'gpt-4o-mini',
            tokenCount: 680,
            inputToken: 460,
            outputToken: 220,
            latencyMs: 2100,
            resultStatus: 'success',
            userAction: 'git_log_panel',
            createdAt: 1737350800456,
            projectName: 'zeka-idea-plugin',
            deviceId: 'dev-1'
        },
        {
            pluginId: 'javadoc',
            eventType: 'javadoc_method',
            provider: 'qianwen',
            model: 'qwen2.5-coder-7b',
            tokenCount: 520,
            inputToken: 320,
            outputToken: 200,
            latencyMs: 1250,
            resultStatus: 'success',
            userAction: 'project_tree_shortcut_file',
            createdAt: 1737437200456,
            projectName: 'intelli-ai-engine',
            deviceId: 'dev-1'
        }
    ]
};

type EventRecord = typeof SAMPLE_DATA.records[number];

type EventStat30mDTO = {
    bucketStart: string | number;
    bucketEnd: string | number;
    deviceId: string;
    projectName: string;
    pluginId: string;
    eventType: string;
    provider: string;
    model: string;
    userAction: string;
    totalCount: number;
    successCount: number;
    failedCount: number;
    tokenTotal: number;
    inputTokenTotal: number;
    outputTokenTotal: number;
    latencyTotalMs: number;
    latencyAvgMs: number;
    latencyMaxMs: number;
    latencyMinMs: number;
};

type EventStatOverviewDTO = {
    totalCount: number;
    successCount: number;
    failedCount: number;
    tokenTotal: number;
    inputTokenTotal: number;
    outputTokenTotal: number;
    latencyAvgMs: number;
    latencyMaxMs: number;
    latencyMinMs: number;
    countByEventType: Record<string, number>;
    tokenByEventType: Record<string, number>;
    countByProvider: Record<string, number>;
    countByUserAction: Record<string, number>;
    tokenByProject: Record<string, number>;
    countByProject: Record<string, number>;
    countByDay: Record<string, number>;
    tokenByDay: Record<string, number>;
    countByDayEventType: Record<string, Record<string, number>>;
    recentBuckets: EventStat30mDTO[];
};

const StatCard = ({icon: Icon, label, value, trend, trendUp}: {
    icon: React.ComponentType<{ className?: string }>,
    label: string,
    value: string,
    trend?: string,
    trendUp?: boolean
}) => (
    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
        <div className="flex items-center justify-between mb-4">
            <div className="p-2 bg-indigo-50 rounded-lg">
                <Icon className="w-6 h-6 text-indigo-600"/>
            </div>
            {trend && (
                <span className={`text-sm font-medium ${trendUp ? 'text-emerald-600' : 'text-red-500'} flex items-center`}>
          {trendUp ? '+' : ''}{trend}
                    <TrendingUp className={`w-3 h-3 ml-1 ${!trendUp && 'rotate-180'}`}/>
        </span>
            )}
        </div>
        <h3 className="text-gray-500 text-sm font-medium mb-1">{label}</h3>
        <p className="text-2xl font-bold text-gray-900">{value}</p>
    </div>
);

export const Statistics: React.FC = () => {
    const buildOverviewFromRecords = (records: EventRecord[]): EventStatOverviewDTO => {
        const totalCount = records.length;
        const successCount = records.filter((r) => r.resultStatus === 'success').length;
        const failedCount = totalCount - successCount;
        const tokenTotal = records.reduce((acc, r) => acc + (r.tokenCount || 0), 0);
        const inputTokenTotal = records.reduce((acc, r) => acc + (r.inputToken || 0), 0);
        const outputTokenTotal = records.reduce((acc, r) => acc + (r.outputToken || 0), 0);
        const latencyAvgMs = totalCount
            ? Math.round(records.reduce((acc, r) => acc + (r.latencyMs || 0), 0) / totalCount)
            : 0;
        const latencyMaxMs = records.reduce((max, r) => Math.max(max, r.latencyMs || 0), 0);
        const latencyMinMs = records.reduce((min, r) => Math.min(min, r.latencyMs || 0), latencyMaxMs || 0);
        const countByEventType: Record<string, number> = {};
        const tokenByEventType: Record<string, number> = {};
        const countByProvider: Record<string, number> = {};
        const countByUserAction: Record<string, number> = {};
        const tokenByProject: Record<string, number> = {};
        const countByProject: Record<string, number> = {};
        const countByDay: Record<string, number> = {};
        const tokenByDay: Record<string, number> = {};
        const countByDayEventType: Record<string, Record<string, number>> = {};
        records.forEach((r) => {
            countByEventType[r.eventType] = (countByEventType[r.eventType] || 0) + 1;
            tokenByEventType[r.eventType] = (tokenByEventType[r.eventType] || 0) + (r.tokenCount || 0);
            countByProvider[r.provider] = (countByProvider[r.provider] || 0) + 1;
            countByUserAction[r.userAction] = (countByUserAction[r.userAction] || 0) + 1;
            tokenByProject[r.projectName || 'unknown'] = (tokenByProject[r.projectName || 'unknown'] || 0) + (r.tokenCount || 0);
            countByProject[r.projectName || 'unknown'] = (countByProject[r.projectName || 'unknown'] || 0) + 1;
            const day = new Date(r.createdAt).toISOString().slice(0, 10);
            countByDay[day] = (countByDay[day] || 0) + 1;
            tokenByDay[day] = (tokenByDay[day] || 0) + (r.tokenCount || 0);
            countByDayEventType[day] = countByDayEventType[day] || {};
            countByDayEventType[day][r.eventType] = (countByDayEventType[day][r.eventType] || 0) + 1;
        });
        const recentBuckets: EventStat30mDTO[] = records
            .slice()
            .sort((a, b) => b.createdAt - a.createdAt)
            .slice(0, 6)
            .map((r) => ({
                bucketStart: r.createdAt,
                bucketEnd: r.createdAt,
                deviceId: r.deviceId,
                projectName: r.projectName,
                pluginId: r.pluginId,
                eventType: r.eventType,
                provider: r.provider,
                model: r.model,
                userAction: r.userAction,
                totalCount: 1,
                successCount: r.resultStatus === 'success' ? 1 : 0,
                failedCount: r.resultStatus === 'success' ? 0 : 1,
                tokenTotal: r.tokenCount,
                inputTokenTotal: r.inputToken,
                outputTokenTotal: r.outputToken,
                latencyTotalMs: r.latencyMs,
                latencyAvgMs: r.latencyMs,
                latencyMaxMs: r.latencyMs,
                latencyMinMs: r.latencyMs
            }));
        return {
            totalCount,
            successCount,
            failedCount,
            tokenTotal,
            inputTokenTotal,
            outputTokenTotal,
            latencyAvgMs,
            latencyMaxMs,
            latencyMinMs,
            countByEventType,
            tokenByEventType,
            countByProvider,
            countByUserAction,
            tokenByProject,
            countByProject,
            countByDay,
            tokenByDay,
            countByDayEventType,
            recentBuckets
        };
    };

    const [overview, setOverview] = useState<EventStatOverviewDTO>(
        buildOverviewFromRecords(SAMPLE_DATA.records)
    );
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const deviceId = params.get('deviceId');
        if (!deviceId) {
            return;
        }
        const fetchOverview = async () => {
            setLoading(true);
            try {
                const response = await fetch(`/api/plugin/event-stat30/overview?deviceId=${encodeURIComponent(deviceId)}`);
                const json = await response.json();
                const data = json?.data ?? json;
                if (data && typeof data === 'object') {
                    setOverview(data);
                }
            } catch (e) {
                console.warn('统计数据获取失败，使用本地示例数据', e);
            } finally {
                setLoading(false);
            }
        };
        fetchOverview();
    }, []);

    const totalEvents = overview.totalCount;
    const successRate = totalEvents ? Math.round((overview.successCount / totalEvents) * 100) : 0;
    const totalTokens = overview.tokenTotal;
    const totalInputTokens = overview.inputTokenTotal;
    const totalOutputTokens = overview.outputTokenTotal;
    const avgLatency = overview.latencyAvgMs;
    const byEventType = overview.countByEventType || {};
    const byEventTypeTokens = overview.tokenByEventType || {};
    const byProvider = overview.countByProvider || {};
    const byUserAction = overview.countByUserAction || {};
    const byProjectTokens = overview.tokenByProject || {};
    const byProjectEvents = overview.countByProject || {};
    const byDay = overview.countByDay || {};
    const byDayTokens = overview.tokenByDay || {};
    const byDayAndEvent = overview.countByDayEventType || {};
    const eventTypes = Object.keys(byEventType);
    const dayKeys = Object.keys(byDay).sort();
    const maxDayCount = Math.max(...Object.values(byDay), 1);
    const maxDayTokens = Math.max(...Object.values(byDayTokens), 1);
    const recentRecords = overview.recentBuckets || [];
    const latestBucketTime = recentRecords[0]?.bucketStart;

    const formatDateTime = (value: string | number) => {
        const date = typeof value === 'number' ? new Date(value) : new Date(value);
        return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString();
    };

    const formatBucketRange = (start: string | number, end: string | number) => {
        const startText = formatDateTime(start);
        const endText = formatDateTime(end);
        if (startText === '-' && endText === '-') {
            return '-';
        }
        if (startText === endText) {
            return startText;
        }
        return `${startText} ~ ${endText}`;
    };

    return (
        <div className="min-h-screen bg-[#F9FAFB] p-6 font-sans">
            <div className="max-w-6xl mx-auto">
                <div className="mb-8">
                    <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
                        <BarChart3 className="w-7 h-7 text-indigo-600"/>
                        使用统计
                    </h1>
                    <p className="text-gray-500 mt-1">
                        {loading ? '正在获取统计数据...' : '已接入 zeka-stack-api 统计接口（无 deviceId 时展示示例数据）'}
                    </p>
                </div>

                {/* Key Metrics Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                    <StatCard
                        icon={Code2}
                        label="统计事件数"
                        value={`${totalEvents}`}
                    />
                    <StatCard
                        icon={Zap}
                        label="成功率"
                        value={`${successRate}%`}
                    />
                    <StatCard
                        icon={Clock}
                        label="平均耗时"
                        value={`${avgLatency} ms`}
                    />
                    <StatCard
                        icon={Calendar}
                        label="Token 总消耗"
                        value={`${totalTokens}`}
                    />
                </div>

                {/* Usage Overview */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <Layers className="w-5 h-5 text-indigo-600"/>
                                事件类型分布
                            </h3>
                        </div>
                        <div className="space-y-3">
                            {Object.entries(byEventType).map(([key, value]) => (
                                <div key={key}>
                                    <div className="flex items-center justify-between text-sm text-gray-600">
                                        <span>{key}</span>
                                        <span className="font-medium text-gray-900">{value}</span>
                                    </div>
                                    <div className="h-2 bg-indigo-50 rounded-full overflow-hidden mt-2">
                                        <div
                                            className="h-2 bg-indigo-500/80"
                                            style={{width: `${Math.round((value / totalEvents) * 100)}%`}}
                                        />
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <Cpu className="w-5 h-5 text-indigo-600"/>
                                服务商使用占比
                            </h3>
                        </div>
                        <div className="space-y-3">
                            {Object.entries(byProvider).map(([key, value]) => (
                                <div key={key} className="flex items-center justify-between text-sm">
                                    <span className="text-gray-600">{key}</span>
                                    <span className="font-medium text-gray-900">{value}</span>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <TrendingUp className="w-5 h-5 text-indigo-600"/>
                                入口来源分布
                            </h3>
                        </div>
                        <div className="space-y-3">
                            {Object.entries(byUserAction).map(([key, value]) => (
                                <div key={key} className="flex items-center justify-between text-sm">
                                    <span className="text-gray-600">{key}</span>
                                    <span className="font-medium text-gray-900">{value}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Token Breakdown */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm lg:col-span-2">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <PieChart className="w-5 h-5 text-indigo-600"/>
                                Token 消耗构成（输入/输出）
                            </h3>
                        </div>
                        <div className="flex flex-col sm:flex-row sm:items-center gap-6">
                            <div className="flex-1">
                                <div className="h-3 bg-indigo-50 rounded-full overflow-hidden">
                                    <div
                                        className="h-3 bg-indigo-500/80"
                                        style={{width: `${Math.round((totalInputTokens / (totalTokens || 1)) * 100)}%`}}
                                    />
                                </div>
                                <div className="mt-3 flex items-center justify-between text-sm text-gray-600">
                                    <span>输入 Token</span>
                                    <span className="font-medium text-gray-900">{totalInputTokens}</span>
                                </div>
                            </div>
                            <div className="flex-1">
                                <div className="h-3 bg-purple-50 rounded-full overflow-hidden">
                                    <div
                                        className="h-3 bg-purple-500/80"
                                        style={{width: `${Math.round((totalOutputTokens / (totalTokens || 1)) * 100)}%`}}
                                    />
                                </div>
                                <div className="mt-3 flex items-center justify-between text-sm text-gray-600">
                                    <span>输出 Token</span>
                                    <span className="font-medium text-gray-900">{totalOutputTokens}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <Activity className="w-5 h-5 text-indigo-600"/>
                                常用操作（Token）
                            </h3>
                        </div>
                        <div className="space-y-3">
                            {Object.entries(byEventTypeTokens)
                                .sort((a, b) => b[1] - a[1])
                                .map(([key, value]) => (
                                    <div key={key}>
                                        <div className="flex items-center justify-between text-sm text-gray-600">
                                            <span>{key}</span>
                                            <span className="font-medium text-gray-900">{value}</span>
                                        </div>
                                        <div className="h-2 bg-indigo-50 rounded-full overflow-hidden mt-2">
                                            <div
                                                className="h-2 bg-indigo-500/80"
                                                style={{width: `${Math.round((value / (totalTokens || 1)) * 100)}%`}}
                                            />
                                        </div>
                                    </div>
                                ))}
                        </div>
                    </div>
                </div>

                {/* Project Dimensions */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <FolderKanban className="w-5 h-5 text-indigo-600"/>
                                项目维度 Token 消耗
                            </h3>
                        </div>
                        <div className="space-y-3">
                            {Object.entries(byProjectTokens).map(([key, value]) => (
                                <div key={key}>
                                    <div className="flex items-center justify-between text-sm text-gray-600">
                                        <span>{key}</span>
                                        <span className="font-medium text-gray-900">{value}</span>
                                    </div>
                                    <div className="h-2 bg-indigo-50 rounded-full overflow-hidden mt-2">
                                        <div
                                            className="h-2 bg-indigo-500/80"
                                            style={{width: `${Math.round((value / (totalTokens || 1)) * 100)}%`}}
                                        />
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <FolderKanban className="w-5 h-5 text-indigo-600"/>
                                项目维度事件次数
                            </h3>
                        </div>
                        <div className="space-y-3">
                            {Object.entries(byProjectEvents).map(([key, value]) => (
                                <div key={key} className="flex items-center justify-between text-sm">
                                    <span className="text-gray-600">{key}</span>
                                    <span className="font-medium text-gray-900">{value}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Daily Activity */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <Calendar className="w-5 h-5 text-indigo-600"/>
                                按天统计（次数）
                            </h3>
                        </div>
                        <div className="h-52 flex items-end justify-between gap-2">
                            {dayKeys.map((day) => (
                                <div key={day} className="flex-1 flex flex-col items-center">
                                    <div className="w-full bg-indigo-50 rounded-t-lg relative">
                                        <div
                                            className="absolute bottom-0 left-0 right-0 bg-indigo-500/80 rounded-t-lg"
                                            style={{height: `${Math.round((byDay[day] / maxDayCount) * 100)}%`}}
                                        />
                                    </div>
                                    <span className="text-[10px] text-gray-400 mt-2">{day.slice(5)}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <Calendar className="w-5 h-5 text-indigo-600"/>
                                按天统计（Token）
                            </h3>
                        </div>
                        <div className="h-52 flex items-end justify-between gap-2">
                            {dayKeys.map((day) => (
                                <div key={day} className="flex-1 flex flex-col items-center">
                                    <div className="w-full bg-purple-50 rounded-t-lg relative">
                                        <div
                                            className="absolute bottom-0 left-0 right-0 bg-purple-500/80 rounded-t-lg"
                                            style={{height: `${Math.round((byDayTokens[day] / maxDayTokens) * 100)}%`}}
                                        />
                                    </div>
                                    <span className="text-[10px] text-gray-400 mt-2">{day.slice(5)}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Daily Event Type Counts */}
                <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm mb-8">
                    <div className="flex items-center justify-between mb-6">
                        <h3 className="font-bold text-gray-900 flex items-center gap-2">
                            <Activity className="w-5 h-5 text-indigo-600"/>
                            按天统计（各事件类型次数）
                        </h3>
                    </div>
                    <div className="space-y-4">
                        {dayKeys.map((day) => (
                            <div key={day}>
                                <div className="text-xs text-gray-500 mb-2">{day}</div>
                                <div className="flex items-center gap-2">
                                    {eventTypes.map((type) => {
                                        const value = byDayAndEvent[day]?.[type] || 0;
                                        const width = Math.round((value / (byDay[day] || 1)) * 100);
                                        return (
                                            <div
                                                key={`${day}-${type}`}
                                                className="h-2 rounded-full"
                                                style={{
                                                    width: `${width}%`,
                                                    backgroundColor: type.includes('javadoc')
                                                        ? 'rgba(99,102,241,0.8)'
                                                        : type.includes('commit')
                                                            ? 'rgba(16,185,129,0.8)'
                                                            : 'rgba(168,85,247,0.8)'
                                                }}
                                                title={`${type}: ${value}`}
                                            />
                                        );
                                    })}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Recent Records */}
                <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm mb-8">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="font-bold text-gray-900">最近事件</h3>
                        <span className="text-xs text-gray-400">
                            最新窗口：{latestBucketTime ? formatDateTime(latestBucketTime) : '-'}
                        </span>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="min-w-full text-sm">
                            <thead className="text-gray-500">
                            <tr className="border-b border-gray-100">
                                <th className="text-left py-2 pr-4">时间窗口</th>
                                <th className="text-left py-2 pr-4">事件类型</th>
                                <th className="text-left py-2 pr-4">服务商/模型</th>
                                <th className="text-left py-2 pr-4">耗时</th>
                                <th className="text-left py-2 pr-4">Token</th>
                                <th className="text-left py-2 pr-4">入口</th>
                                <th className="text-left py-2 pr-4">成功/失败</th>
                            </tr>
                            </thead>
                            <tbody className="text-gray-700">
                            {recentRecords.map((item, idx) => (
                                <tr key={`${item.bucketStart}-${idx}`} className="border-b border-gray-50">
                                    <td className="py-2 pr-4 text-gray-500">{formatBucketRange(item.bucketStart, item.bucketEnd)}</td>
                                    <td className="py-2 pr-4">{item.eventType}</td>
                                    <td className="py-2 pr-4">{item.provider}/{item.model}</td>
                                    <td className="py-2 pr-4">{item.latencyAvgMs} ms</td>
                                    <td className="py-2 pr-4">{item.tokenTotal}</td>
                                    <td className="py-2 pr-4">{item.userAction}</td>
                                    <td className={`py-2 pr-4 ${item.failedCount > 0 ? 'text-red-500' : 'text-emerald-600'}`}>
                                        {item.successCount}/{item.failedCount}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
};
