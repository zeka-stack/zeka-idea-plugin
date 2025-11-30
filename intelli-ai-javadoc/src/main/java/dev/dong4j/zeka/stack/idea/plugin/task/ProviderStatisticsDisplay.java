package dev.dong4j.zeka.stack.idea.plugin.task;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import javax.swing.SwingUtilities;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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
    public static void showProviderStatistics(@NotNull Map<String, ProviderStatistics> providerStats) {
        // 创建HTML格式的统计信息
        StringBuilder htmlContent = new StringBuilder();
        // formatter:off
        htmlContent.append("<html><head><style>");
        htmlContent.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 10px; font-size: 12px; }");
        htmlContent.append("h2 { color: #2E7D32; margin-bottom: 15px; font-size: 16px; }");
        htmlContent.append("h3 { color: #1976D2; margin-bottom: 10px; font-size: 14px; }");
        htmlContent.append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; font-size: 11px; border: 1px solid #ddd; }");
        htmlContent.append("th { background-color:rgb(122, 127, 131); color: white; padding: 8px; text-align: center; font-weight: bold; font-size: 11px; border: 1px solid #ddd; }");
        htmlContent.append("td { padding: 8px; text-align: center; font-size: 11px; border: 1px solid #ddd; }");
        htmlContent.append("td.provider-name { text-align: left; }");
        htmlContent.append("tr:nth-child(even) { background-color: #f8f9fa; }");
        htmlContent.append("tr:hover { background-color: #e3f2fd; }");
        htmlContent.append(".summary-row { background-color:rgb(41, 96, 123); color: white; font-weight: bold; }");
        htmlContent.append(".summary-row td { border: 1px solid #ddd; }");
        htmlContent.append("</style></head><body>");
        // formatter:on
        // 添加标题
        htmlContent.append("<h2>🚀 性能模式处理完成</h2>");

        // 创建提供商统计表格
        htmlContent.append("<table>");
        htmlContent.append("<tr><th>服务商名称</th><th>完成数量</th><th>失败数量</th><th>跳过数量</th><th>耗时</th></tr>");

        int totalCompleted = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        long totalDuration = 0;

        for (ProviderStatistics stats : providerStats.values()) {
            htmlContent.append("<tr>");
            htmlContent.append("<td class='provider-name'>").append(stats.getProviderName()).append("</td>");
            htmlContent.append("<td>").append(stats.getCompletedCount()).append("</td>");
            htmlContent.append("<td>").append(stats.getFailedCount()).append("</td>");
            htmlContent.append("<td>").append(stats.getSkippedCount()).append("</td>");
            htmlContent.append("<td>").append(String.format("%.1fs", stats.getDuration() / 1000.0)).append("</td>");
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
        htmlContent.append("</tr>");

        htmlContent.append("</table>");
        htmlContent.append("</body></html>");

        // 在日志中记录详细信息
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("性能模式统计详情：\n");
        logMessage.append("各提供商处理统计：\n");

        for (ProviderStatistics stats : providerStats.values()) {
            logMessage.append("• ").append(stats.toString()).append("\n");
        }

        logMessage.append("\n总体统计：\n");
        logMessage.append(String.format("• 总计: %d 个任务\n", totalCompleted + totalFailed + totalSkipped));
        logMessage.append(String.format("• 完成: %d 个\n", totalCompleted));
        logMessage.append(String.format("• 失败: %d 个\n", totalFailed));
        logMessage.append(String.format("• 跳过: %d 个\n", totalSkipped));
        logMessage.append(String.format("• 总耗时: %.1f 秒\n", totalDuration / 1000.0));

        if (totalCompleted > 0) {
            double avgTimePerTask = (double) totalDuration / totalCompleted;
            logMessage.append(String.format("• 平均每任务耗时: %.1f 秒", avgTimePerTask / 1000.0));
        }

        log.info("{}", logMessage);

        // 显示HTML格式的通知给用户
        SwingUtilities.invokeLater(() -> {
            // 创建自定义对话框（非模态）
            javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) null, "性能模式处理完成", false);
            dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);

            // 创建HTML内容面板
            javax.swing.JEditorPane editorPane = new javax.swing.JEditorPane();
            editorPane.setContentType("text/html");
            editorPane.setText(htmlContent.toString());
            editorPane.setEditable(false);
            editorPane.setBackground(javax.swing.UIManager.getColor("Panel.background"));

            // 计算动态高度
            int providerCount = providerStats.size();

            // 每行高度约30px，表头高度约35px，总体统计行高度约35px, 在加上标题和一定的冗余量
            int calculatedHeight = 35 + (providerCount * 30) + 35 + 170;

            // 设置最小和最大高度阈值
            int minHeight = 200;  // 最小高度
            int maxHeight = 800;  // 最大高度
            // 应用阈值限制
            int finalHeight = Math.max(minHeight, Math.min(maxHeight, calculatedHeight));

            // 记录高度计算信息
            log.debug("动态高度计算: 提供商数量={}, 计算高度={}, 最终高度={}",
                      providerCount, calculatedHeight, finalHeight);

            // 设置滚动面板
            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(editorPane);
            scrollPane.setPreferredSize(new java.awt.Dimension(800, finalHeight));

            // 添加确定按钮
            javax.swing.JButton okButton = new javax.swing.JButton("确定");
            okButton.addActionListener(e -> dialog.dispose());

            javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
            buttonPanel.add(okButton);

            // 设置布局
            dialog.setLayout(new java.awt.BorderLayout());
            dialog.add(scrollPane, java.awt.BorderLayout.CENTER);
            dialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);

            // 设置对话框属性
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
    }
}

