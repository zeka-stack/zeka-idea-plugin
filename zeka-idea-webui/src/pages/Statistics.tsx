import React from 'react';
import {BarChart3, Calendar, Clock, Code2, TrendingUp, Zap} from 'lucide-react';

const StatCard = ({icon: Icon, label, value, trend, trendUp}: {
    icon: any,
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
    return (
        <div className="min-h-screen bg-[#F9FAFB] p-6 font-sans">
            <div className="max-w-6xl mx-auto">
                <div className="mb-8">
                    <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
                        <BarChart3 className="w-7 h-7 text-indigo-600"/>
                        使用统计
                    </h1>
                    <p className="text-gray-500 mt-1">
                        查看您使用 Zeka Engine 的效率提升数据
                    </p>
                </div>

                {/* Key Metrics Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                    <StatCard
                        icon={Code2}
                        label="代码生成行数"
                        value="12,458"
                        trend="12%"
                        trendUp={true}
                    />
                    <StatCard
                        icon={Zap}
                        label="节省时间 (估算)"
                        value="48 小时"
                        trend="5%"
                        trendUp={true}
                    />
                    <StatCard
                        icon={Clock}
                        label="每日平均使用"
                        value="4.2 小时"
                        trend="2%"
                        trendUp={false}
                    />
                    <StatCard
                        icon={Calendar}
                        label="连续使用天数"
                        value="15 天"
                    />
                </div>

                {/* Charts Section (Placeholders) */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <div className="lg:col-span-2 bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="font-bold text-gray-900">代码生成趋势</h3>
                            <select className="text-sm border-gray-200 rounded-lg text-gray-600 focus:ring-indigo-500 focus:border-indigo-500">
                                <option>最近 7 天</option>
                                <option>最近 30 天</option>
                            </select>
                        </div>
                        {/* Placeholder for Line Chart */}
                        <div className="h-64 flex items-end justify-between gap-2 px-2">
                            {[40, 65, 45, 80, 55, 90, 75].map((h, i) => (
                                <div key={i} className="w-full bg-indigo-50 rounded-t-lg relative group">
                                    <div
                                        className="absolute bottom-0 left-0 right-0 bg-indigo-500/80 rounded-t-lg transition-all duration-500 hover:bg-indigo-600"
                                        style={{height: `${h}%`}}
                                    ></div>
                                    <div className="absolute -bottom-6 left-1/2 -translate-x-1/2 text-xs text-gray-400">
                                        {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][i]}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm">
                        <h3 className="font-bold text-gray-900 mb-6">语言分布</h3>
                        {/* Placeholder for Pie/Donut Chart */}
                        <div className="relative h-48 w-48 mx-auto mb-6">
                            <svg viewBox="0 0 100 100" className="w-full h-full transform -rotate-90">
                                <circle cx="50" cy="50" r="40" stroke="#EEF2FF" strokeWidth="20" fill="none"/>
                                <circle cx="50" cy="50" r="40" stroke="#6366F1" strokeWidth="20" fill="none" strokeDasharray="180 251"/>
                                <circle cx="50" cy="50" r="40" stroke="#A855F7" strokeWidth="20" fill="none" strokeDasharray="50 251" strokeDashoffset="-180"/>
                            </svg>
                            <div className="absolute inset-0 flex items-center justify-center flex-col">
                                <span className="text-2xl font-bold text-gray-900">Total</span>
                            </div>
                        </div>
                        <div className="space-y-3">
                            <div className="flex items-center justify-between text-sm">
                                <div className="flex items-center gap-2">
                                    <div className="w-3 h-3 rounded-full bg-indigo-500"></div>
                                    <span className="text-gray-600">Java</span>
                                </div>
                                <span className="font-medium text-gray-900">72%</span>
                            </div>
                            <div className="flex items-center justify-between text-sm">
                                <div className="flex items-center gap-2">
                                    <div className="w-3 h-3 rounded-full bg-purple-500"></div>
                                    <span className="text-gray-600">Kotlin</span>
                                </div>
                                <span className="font-medium text-gray-900">20%</span>
                            </div>
                            <div className="flex items-center justify-between text-sm">
                                <div className="flex items-center gap-2">
                                    <div className="w-3 h-3 rounded-full bg-gray-200"></div>
                                    <span className="text-gray-600">Other</span>
                                </div>
                                <span className="font-medium text-gray-900">8%</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};
