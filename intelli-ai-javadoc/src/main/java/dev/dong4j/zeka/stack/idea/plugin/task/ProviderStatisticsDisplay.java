package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * 提供商统计信息显示类
 * <p>
 * 负责显示提供商的统计信息，包括HTML格式的表格和日志信息。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
public final class ProviderStatisticsDisplay {

    private ProviderStatisticsDisplay() {
        // 工具类，禁止实例化
    }

    /**
     * 显示提供商的统计信息，包括HTML格式的表格和日志信息。
     * <p>
     * 该方法接收一个包含提供商统计信息的Map，生成HTML格式的统计表格，并在日志中记录详细信息。
     * 同时，会弹出一个对话框展示统计结果。
     *
     * @param providerStats 包含提供商统计信息的Map，键为服务商名称，值为对应的统计对象
     */
    public static void showProviderStatistics(Project project, @NotNull Map<String, ProviderStatistics> providerStats) {
        // 根据当前 IDE 主题颜色构建 HTML 样式，兼容明亮/暗色模式
        Color panelBg = UIManager.getColor("Panel.background");
        Color labelFg = UIManager.getColor("Label.foreground");
        Color tableBg = UIManager.getColor("Table.background");
        Color tableFg = UIManager.getColor("Table.foreground");
        Color tableGrid = UIManager.getColor("Table.gridColor");
        Color headerBg = UIManager.getColor("TableHeader.background");
        Color headerFg = UIManager.getColor("TableHeader.foreground");

        // 先得到用于计算的 Color，再统一转成十六进制，避免出现 Serializable 推断错误
        Color bodyBgColor = panelBg != null ? panelBg : JBColor.WHITE;
        Color bodyFgColor = labelFg != null ? labelFg : JBColor.BLACK;
        Color tableBgColor = tableBg != null ? tableBg : bodyBgColor;
        Color tableFgColor = tableFg != null ? tableFg : bodyFgColor;
        Color gridColor = tableGrid != null ? tableGrid : Gray._68;
        Color headerBgColor = headerBg != null ? headerBg : tableBgColor;
        Color headerFgColor = headerFg != null ? headerFg : bodyFgColor;

        String bodyBgHex = toHex(bodyBgColor);
        String bodyFgHex = toHex(bodyFgColor);
        String tableBgHex = toHex(tableBgColor);
        String tableFgHex = toHex(tableFgColor);
        String gridHex = toHex(gridColor);
        String headerBgHex = toHex(headerBgColor);
        String headerFgHex = toHex(headerFgColor);

        // 创建HTML格式的统计信息
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><head><style>");
        // 使用 IDE 当前主题的前景/背景色，避免在暗色模式下文字不可见
        htmlContent.append("body {")
            .append("margin: 10px; font-family: 'Segoe UI', Arial, sans-serif; font-size: 12px;")
            .append("color: ").append(bodyFgHex).append(";")
            .append("background-color: ").append(bodyBgHex).append(";")
            .append("}");
        htmlContent.append("h2 { margin-bottom: 12px; font-size: 16px; font-weight: bold; color: ")
            .append(bodyFgHex).append("; }");
        htmlContent.append("h3 { margin-bottom: 8px; font-size: 14px; font-weight: bold; color: ")
            .append(bodyFgHex).append("; }");
        htmlContent.append("table { border-collapse: collapse; width: 100%; margin-bottom: 16px; font-size: 11px; ")
            .append("background-color: ").append(tableBgHex).append(";")
            .append("color: ").append(tableFgHex).append(";")
            .append("border: 1px solid ").append(gridHex).append(";")
            .append(" }");
        htmlContent.append("th { padding: 6px 8px; text-align: center; font-weight: bold; font-size: 11px; ")
            .append("background-color: ").append(headerBgHex).append(";")
            .append("color: ").append(headerFgHex).append(";")
            .append("border: 1px solid ").append(gridHex).append(";")
            .append(" }");
        htmlContent.append("td { padding: 6px 8px; text-align: center; font-size: 11px; ")
            .append("border: 1px solid ").append(gridHex).append(";")
            .append(" }");
        // 第一列（服务商名称）也居中显示，统一风格
        htmlContent.append("td.provider-name { text-align: center; font-weight: 600; }");
        // 总体统计行使用表头背景色以保证在暗色模式下可读
        htmlContent.append(".summary-row { font-weight: bold; background-color: ").append(headerBgHex)
            .append("; color: ").append(headerFgHex).append("; }");
        htmlContent.append(".summary-row td { border: 1px solid ").append(gridHex).append("; }");
        htmlContent.append("</style></head><body>");
        htmlContent.append("<h2>🚀 性能模式处理完成</h2>");

        // 创建提供商统计表格
        htmlContent.append("<table>");
        htmlContent.append("<tr><th>服务商名称</th><th>完成数量</th><th>失败数量</th><th>跳过数量</th><th>耗时</th><th>成功率</th><th>吞吐量(条/秒)</th></tr>");

        int totalCompleted = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        long totalDuration = 0;

        for (ProviderStatistics stats : providerStats.values()) {
            long handled = stats.getCompletedCount() + stats.getFailedCount();
            double successRate = handled == 0 ? 0.0 : (double) stats.getCompletedCount() / handled;
            double durationSec = stats.getDuration() / 1000.0;
            double throughput = durationSec <= 0 ? 0.0 : stats.getCompletedCount() / durationSec;

            htmlContent.append("<tr>");
            htmlContent.append("<td class='provider-name'>").append(stats.getProviderName()).append("</td>");
            htmlContent.append("<td>").append(stats.getCompletedCount()).append("</td>");
            htmlContent.append("<td>").append(stats.getFailedCount()).append("</td>");
            htmlContent.append("<td>").append(stats.getSkippedCount()).append("</td>");
            htmlContent.append("<td>").append(String.format("%.1fs", stats.getDuration() / 1000.0)).append("</td>");
            htmlContent.append("<td>").append(String.format("%.1f%%", successRate * 100)).append("</td>");
            htmlContent.append("<td>").append(String.format("%.2f", throughput)).append("</td>");
            htmlContent.append("</tr>");

            totalCompleted += stats.getCompletedCount();
            totalFailed += stats.getFailedCount();
            totalSkipped += stats.getSkippedCount();
            totalDuration += stats.getDuration();
        }

        // 添加总体统计行
        htmlContent.append("<tr class='summary-row'>");
        htmlContent.append("<td>📊 总体统计</td>");
        htmlContent.append("<td>").append(totalCompleted).append("</td>");
        htmlContent.append("<td>").append(totalFailed).append("</td>");
        htmlContent.append("<td>").append(totalSkipped).append("</td>");
        htmlContent.append("<td>").append(String.format("%.1fs", totalDuration / 1000.0)).append("</td>");
        double totalHandled = totalCompleted + totalFailed;
        double totalSuccessRate = totalHandled == 0 ? 0.0 : totalCompleted / totalHandled;
        double totalDurationSec = totalDuration / 1000.0;
        double totalThroughput = totalDurationSec <= 0 ? 0.0 : totalCompleted / totalDurationSec;
        htmlContent.append("<td>").append(String.format("%.1f%%", totalSuccessRate * 100)).append("</td>");
        htmlContent.append("<td>").append(String.format("%.2f", totalThroughput)).append("</td>");
        htmlContent.append("</tr>");

        htmlContent.append("</table>");
        htmlContent.append("</div>");
        htmlContent.append("</body></html>");

        // 在日志中记录详细信息
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("性能模式统计详情：\n");
        logMessage.append("各提供商处理统计：\n");

        for (ProviderStatistics stats : providerStats.values()) {
            long handled = stats.getCompletedCount() + stats.getFailedCount();
            double successRate = handled == 0 ? 0.0 : (double) stats.getCompletedCount() / handled;
            double durationSec = stats.getDuration() / 1000.0;
            double throughput = durationSec <= 0 ? 0.0 : stats.getCompletedCount() / durationSec;
            logMessage.append(String.format("• %s | 完成:%d 失败:%d 跳过:%d 耗时:%.1fs 成功率:%.1f%% 吞吐量:%.2f条/秒%n",
                                            stats.getProviderName(),
                                            stats.getCompletedCount(),
                                            stats.getFailedCount(),
                                            stats.getSkippedCount(),
                                            durationSec,
                                            successRate * 100,
                                            throughput));
        }

        logMessage.append("\n总体统计：\n");
        logMessage.append(String.format("• 总计: %d 个任务\n", totalCompleted + totalFailed + totalSkipped));
        logMessage.append(String.format("• 完成: %d 个\n", totalCompleted));
        logMessage.append(String.format("• 失败: %d 个\n", totalFailed));
        logMessage.append(String.format("• 跳过: %d 个\n", totalSkipped));
        logMessage.append(String.format("• 总耗时: %.1f 秒\n", totalDuration / 1000.0));

        if (totalCompleted > 0) {
            double avgTimePerTask = (double) totalDuration / totalCompleted;
            logMessage.append(String.format("• 平均每任务耗时: %.1f 秒%n", avgTimePerTask / 1000.0));
        }
        double handled = totalCompleted + totalFailed;
        if (handled > 0) {
            totalSuccessRate = totalCompleted / handled;
            totalThroughput = totalDuration > 0 ? totalCompleted / (totalDuration / 1000.0) : 0.0;
            logMessage.append(String.format("• 总体成功率: %.1f%%%n", totalSuccessRate * 100));
            logMessage.append(String.format("• 总体吞吐量: %.2f 条/秒", totalThroughput));
        }

        AIConsoleLoggerUtil.print(project, logMessage.toString());

        // 显示HTML格式的通知给用户
        SwingUtilities.invokeLater(() -> {
            // 创建自定义对话框（非模态）
            JDialog dialog = new JDialog((Frame) null, "性能模式处理完成", false);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            // 创建HTML内容面板
            JEditorPane editorPane = new JEditorPane();
            editorPane.setContentType("text/html");
            editorPane.setText(htmlContent.toString());
            editorPane.setEditable(false);
            editorPane.setBackground(UIManager.getColor("Panel.background"));

            // 计算动态高度
            int providerCount = providerStats.size();

            // 每行高度约30px，表头高度约35px，总体统计行高度约35px, 在加上标题和一定的冗余量
            int calculatedHeight = 35 + (providerCount * 30) + 35 + 170;

            // 设置最小和最大高度阈值
            int minHeight = 200;  // 最小高度
            int maxHeight = 800;  // 最大高度
            // 应用阈值限制
            int finalHeight = Math.max(minHeight, Math.min(maxHeight, calculatedHeight));

            // 设置滚动面板
            JScrollPane scrollPane = new JScrollPane(editorPane);
            scrollPane.setPreferredSize(new Dimension(800, finalHeight));

            // 添加确定按钮
            JButton okButton = new JButton("确定");
            okButton.addActionListener(e -> dialog.dispose());

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(okButton);

            // 设置布局
            dialog.setLayout(new BorderLayout());
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            // 设置对话框属性
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
    }

    /**
     * 将 {@link Color} 转换为 HTML 可用的十六进制颜色字符串（#RRGGBB）。
     *
     * @param color 颜色对象，如果为 null 则返回黑色
     * @return 十六进制颜色字符串
     */
    @NotNull
    private static String toHex(Color color) {
        if (color == null) {
            return "#000000";
        }
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 便于独立运行的演示入口（使用模拟数据直接弹出统计窗口）。
     */
    public static void main(String[] args) {
        long now = System.currentTimeMillis();

        ProviderStatistics p1 = new ProviderStatistics("QianWen", now - 5_000);
        p1.incrementCompleted();
        p1.incrementCompleted();
        p1.incrementFailed();
        p1.finish();

        ProviderStatistics p2 = new ProviderStatistics("Ollama", now - 8_000);
        p2.incrementCompleted();
        p2.incrementCompleted();
        p2.incrementCompleted();
        p2.incrementSkipped();
        p2.finish();

        ProviderStatistics p3 = new ProviderStatistics("Custom", now - 3_500);
        p3.incrementFailed();
        p3.incrementFailed();
        p3.finish();

        Map<String, ProviderStatistics> demo = new java.util.LinkedHashMap<>();
        demo.put("qianwen", p1);
        demo.put("ollama", p2);
        demo.put("custom", p3);

        showProviderStatistics(null, demo);
    }
}
