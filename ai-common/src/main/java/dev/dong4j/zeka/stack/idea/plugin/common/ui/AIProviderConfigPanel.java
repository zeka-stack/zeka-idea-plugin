package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 可复用的 AI 提供商配置面板。
 */
public final class AIProviderConfigPanel {

    private final AICredentialManager credentialManager;
    private final AIResponseListener responseListener;

    private JPanel mainPanel;

    private ComboBox<String> providerComboBox;
    private ComboBox<String> modelComboBox;
    private JBTextField baseUrlField;
    private JBPasswordField apiKeyField;
    private JButton testConnectionButton;
    private JButton refreshModelsButton;

    private JBCheckBox showAvailableProvidersCheckBox;
    private JPanel availableProvidersPanel;
    private JBTable availableProvidersTable;
    private AvailableProvidersTableModel availableProvidersTableModel;

    private Boolean configurationVerified = Boolean.FALSE;
    private Boolean refreshModelsSuccess = null;

    private AIProviderSettings workingSettings = new AIProviderSettings();

    public AIProviderConfigPanel(@NotNull AICredentialManager credentialManager) {
        this(credentialManager, null);
    }

    public AIProviderConfigPanel(@NotNull AICredentialManager credentialManager,
                                 @Nullable AIResponseListener responseListener) {
        this.credentialManager = credentialManager;
        this.responseListener = responseListener;
        createUI();
        setupListeners();
    }

    @NotNull
    public JPanel getPanel() {
        return mainPanel;
    }

    public void loadSettings(@NotNull AIProviderSettings settings) {
        this.workingSettings = settings.copy();

        providerComboBox.setSelectedItem(workingSettings.providerType.getDisplayName());
        updateModelList();

        AIProviderConfig defaultConfig = workingSettings.getDefaultProviderConfig(workingSettings.providerType);
        modelComboBox.setSelectedItem(defaultConfig.modelName);
        baseUrlField.setText(defaultConfig.baseUrl);
        configurationVerified = defaultConfig.configurationVerified;
        updateTestButtonState();

        loadApiKeyAsync(defaultConfig.credentialId, workingSettings.providerType.getProviderId());

        refreshModelsSuccess = null;
        updateRefreshButtonState();

        availableProvidersTableModel.setData(workingSettings.availableProviders);
        boolean visible = !workingSettings.availableProviders.isEmpty();
        showAvailableProvidersCheckBox.setSelected(visible);
        availableProvidersPanel.setVisible(visible);
    }

    @NotNull
    public AIProviderSettings getSettings() {
        AIProviderSettings copy = workingSettings.copy();

        AIProviderType providerType = resolveSelectedProviderType();
        copy.providerType = providerType;

        AIProviderConfig defaultConfig = copy.getDefaultProviderConfig(providerType);
        String modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        defaultConfig.modelName = modelName.isEmpty() ? providerType.getDefaultModel() : modelName;
        defaultConfig.baseUrl = normalizeBaseUrl(baseUrlField.getText());
        defaultConfig.configurationVerified = Boolean.TRUE.equals(configurationVerified);
        defaultConfig.updateCredentialId(getCurrentApiKey());
        copy.updateDefaultProviderConfig(providerType, defaultConfig);

        copy.availableProviders.clear();
        availableProvidersTableModel.getData().forEach(copy::addAvailableProvider);

        return copy;
    }

    public boolean isModified(@NotNull AIProviderSettings baseline) {
        AIProviderSettings latest = getSettings();
        AIProviderSettings expected = baseline.copy();
        // 提供商类型
        if (latest.providerType != expected.providerType) {
            return true;
        }

        AIProviderConfig latestConfig = latest.getDefaultProviderConfig(latest.providerType);
        AIProviderConfig expectedConfig = expected.getDefaultProviderConfig(expected.providerType);
        if (!Objects.equals(latestConfig.modelName, expectedConfig.modelName)) {
            return true;
        }
        if (!Objects.equals(latestConfig.baseUrl, expectedConfig.baseUrl)) {
            return true;
        }
        if (latestConfig.configurationVerified != expectedConfig.configurationVerified) {
            return true;
        }
        if (!Objects.equals(latestConfig.credentialId, expectedConfig.credentialId)) {
            return true;
        }

        if (latest.availableProviders.size() != expected.availableProviders.size()) {
            return true;
        }
        for (int i = 0; i < latest.availableProviders.size(); i++) {
            AIProviderConfig a = latest.availableProviders.get(i);
            AIProviderConfig b = expected.availableProviders.get(i);
            if (!Objects.equals(a.providerType, b.providerType)
                || !Objects.equals(a.modelName, b.modelName)
                || !Objects.equals(a.baseUrl, b.baseUrl)
                || !Objects.equals(a.credentialId, b.credentialId)
                || !Objects.equals(a.remark, b.remark)
                || a.configurationVerified != b.configurationVerified) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public String getCurrentApiKey() {
        return new String(apiKeyField.getPassword()).trim();
    }

    private void createUI() {
        providerComboBox = new ComboBox<>(AIProviderType.getAllDisplayNames().toArray(new String[0]));
        providerComboBox.setRenderer(new ProviderListCellRenderer());

        modelComboBox = new ComboBox<>();
        modelComboBox.setEditable(true);

        baseUrlField = new JBTextField();
        baseUrlField.setToolTipText(AICommonBundle.message("settings.base.url.tooltip"));

        apiKeyField = new JBPasswordField();
        apiKeyField.setToolTipText(AICommonBundle.message("settings.api.key.tooltip"));

        testConnectionButton = new JButton(AICommonBundle.message("settings.test.connection"));
        refreshModelsButton = new JButton(AICommonBundle.message("settings.refresh.models"));
        updateRefreshButtonState();

        availableProvidersTableModel = new AvailableProvidersTableModel();
        availableProvidersTable = new JBTable(availableProvidersTableModel);
        availableProvidersTable.setPreferredScrollableViewportSize(new Dimension(480, 120));
        availableProvidersTable.getColumnModel().getColumn(0).setCellRenderer(new ProviderTableCellRenderer());

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(availableProvidersTable)
            .setRemoveAction(button -> {
                int selected = availableProvidersTable.getSelectedRow();
                if (selected >= 0) {
                    removeAvailableProvider(selected);
                }
            })
            .addExtraAction(new AnAction(AICommonBundle.message("settings.available.providers.clear.all"),
                                         AICommonBundle.message("settings.available.providers.clear.all.description"),
                                         AllIcons.Actions.GC) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    clearAllAvailableProviders();
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.EDT;
                }
            });
        availableProvidersPanel = decorator.createPanel();
        availableProvidersPanel.setVisible(false);

        showAvailableProvidersCheckBox = new JBCheckBox(AICommonBundle.message("settings.show.available.providers"));

        JPanel providerSection = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.provider.label")), providerComboBox)
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.model.label")), createModelPanel())
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.base.url.label")), baseUrlField)
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.api.key.label")), createApiKeyPanel())
            .addSeparator(10)
            .addComponent(showAvailableProvidersCheckBox)
            .addComponent(availableProvidersPanel)
            .getPanel();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(providerSection, BorderLayout.CENTER);
        wrapper.setBorder(new TitledBorder(AICommonBundle.message("settings.basic.connection.config")));

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(wrapper, BorderLayout.NORTH);
        mainPanel.setBorder(JBUI.Borders.empty(8));
    }

    private JPanel createModelPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(modelComboBox, BorderLayout.CENTER);
        panel.add(testConnectionButton, BorderLayout.EAST);
        return panel;
    }

    private JPanel createApiKeyPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(apiKeyField, BorderLayout.CENTER);
        panel.add(refreshModelsButton, BorderLayout.EAST);
        return panel;
    }

    private void setupListeners() {
        providerComboBox.addActionListener(e -> {
            updateModelList();
            loadDefaultProviderConfig();
        });

        showAvailableProvidersCheckBox.addActionListener(e ->
                                                             availableProvidersPanel.setVisible(showAvailableProvidersCheckBox.isSelected()));

        testConnectionButton.addActionListener(e -> testConnection());
        refreshModelsButton.addActionListener(e -> refreshModels());
    }

    private void updateModelList() {
        AIProviderType providerType = resolveSelectedProviderType();
        List<String> models = providerType.getSupportedModels();
        modelComboBox.removeAllItems();
        models.forEach(modelComboBox::addItem);
        if (!models.isEmpty()) {
            modelComboBox.setSelectedItem(models.get(0));
        }
        updateBaseUrlEditable(providerType);
        updateApiKeyEnabled(providerType);
    }

    private void loadDefaultProviderConfig() {
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        modelComboBox.setSelectedItem(config.modelName);
        baseUrlField.setText(config.baseUrl);
        configurationVerified = config.configurationVerified;
        updateTestButtonState();
        loadApiKeyAsync(config.credentialId, providerType.getProviderId());
    }

    private void loadApiKeyAsync(@Nullable String credentialId, @NotNull String expectedProviderId) {
        apiKeyField.setText("");
        if (credentialId == null || credentialId.trim().isEmpty()) {
            return;
        }
        credentialManager.loadApiKeyAsync(credentialId, key -> {
            String currentProviderId = resolveSelectedProviderType().getProviderId();
            if (!Objects.equals(currentProviderId, expectedProviderId)) {
                return;
            }
            apiKeyField.setText(key != null ? key : "");
            updateTestButtonState();
        });
    }

    private void testConnection() {
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderSettings snapshot = workingSettings.copy();
        snapshot.providerType = providerType;
        AIProviderConfig config = snapshot.getDefaultProviderConfig(providerType);
        config.modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        config.baseUrl = normalizeBaseUrl(baseUrlField.getText());
        config.updateCredentialId(getCurrentApiKey());
        snapshot.updateDefaultProviderConfig(providerType, config);

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(config, snapshot.modelParameters, snapshot.runtimeSettings);
            if (provider == null) {
                JOptionPane.showMessageDialog(mainPanel,
                                              AICommonBundle.message("settings.error.provider.create.failed.details"),
                                              AICommonBundle.message("settings.error.title"),
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                                          AICommonBundle.message("settings.error.provider.create.failed"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        testConnectionButton.setEnabled(false);
        testConnectionButton.setText(AICommonBundle.message("settings.test.connection.testing"));
        testConnectionButton.setIcon(createStatusDotIcon(Gray._158));

        new Thread(() -> {
            try {
                ValidationResult result = provider.validateConfiguration(getCurrentApiKey());
                SwingUtilities.invokeLater(() -> {
                    if (result.isSuccess()) {
                        configurationVerified = true;
                        updateTestButtonState();
                        addAvailableProvider(config, providerType);
                        JOptionPane.showMessageDialog(mainPanel,
                                                      result.getMessage(),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        configurationVerified = false;
                        updateTestButtonState();
                        removeAvailableProvider(config.credentialId);
                        JOptionPane.showMessageDialog(mainPanel,
                                                      result.getFullErrorMessage(),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    configurationVerified = false;
                    updateTestButtonState();
                    removeAvailableProvider(config.credentialId);
                    JOptionPane.showMessageDialog(mainPanel,
                                                  AICommonBundle.message("settings.test.connection.error", e.getMessage()),
                                                  AICommonBundle.message("settings.test.result.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    testConnectionButton.setText(AICommonBundle.message("settings.test.connection"));
                    testConnectionButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void refreshModels() {
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderSettings snapshot = workingSettings.copy();
        snapshot.providerType = providerType;
        AIProviderConfig config = snapshot.getDefaultProviderConfig(providerType);
        config.modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        config.baseUrl = normalizeBaseUrl(baseUrlField.getText());
        config.updateCredentialId(getCurrentApiKey());
        snapshot.updateDefaultProviderConfig(providerType, config);

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(config, snapshot.modelParameters, snapshot.runtimeSettings);
            if (provider == null) {
                JOptionPane.showMessageDialog(mainPanel,
                                              AICommonBundle.message("settings.error.provider.create.failed"),
                                              AICommonBundle.message("settings.error.title"),
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                                          AICommonBundle.message("settings.error.provider.create.failed.details"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshModelsButton.setEnabled(false);
        refreshModelsButton.setText(AICommonBundle.message("settings.refresh.models.testing"));
        refreshModelsButton.setIcon(createStatusDotIcon(Gray._158));

        new Thread(() -> {
            try {
                List<String> models = provider.getAvailableModels(getCurrentApiKey());
                models.sort(String::compareToIgnoreCase);
                SwingUtilities.invokeLater(() -> {
                    modelComboBox.removeAllItems();
                    if (!models.isEmpty()) {
                        models.forEach(modelComboBox::addItem);
                        modelComboBox.setSelectedItem(models.get(0));
                        refreshModelsSuccess = true;
                        JOptionPane.showMessageDialog(mainPanel,
                                                      AICommonBundle.message("settings.refresh.models.success", models.size()),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        refreshModelsSuccess = false;
                        JOptionPane.showMessageDialog(mainPanel,
                                                      AICommonBundle.message("settings.refresh.models.empty"),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.WARNING_MESSAGE);
                    }
                    updateRefreshButtonState();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    refreshModelsSuccess = false;
                    updateRefreshButtonState();
                    JOptionPane.showMessageDialog(mainPanel,
                                                  AICommonBundle.message("settings.refresh.models.failed", e.getMessage()),
                                                  AICommonBundle.message("settings.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    refreshModelsButton.setText(AICommonBundle.message("settings.refresh.models"));
                    refreshModelsButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void addAvailableProvider(@NotNull AIProviderConfig config, @NotNull AIProviderType providerType) {
        AIProviderConfig copy = config.copy();
        if (copy.remark == null || copy.remark.isEmpty()) {
            copy.remark = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
        }
        copy.providerType = providerType;
        copy.configurationVerified = true;
        workingSettings.addAvailableProvider(copy);
        availableProvidersTableModel.setData(workingSettings.availableProviders);
        showAvailableProvidersCheckBox.setSelected(true);
        availableProvidersPanel.setVisible(true);
    }

    private void removeAvailableProvider(@Nullable String credentialId) {
        if (credentialId == null || credentialId.trim().isEmpty()) {
            return;
        }
        workingSettings.removeAvailableProvider(credentialId);
        availableProvidersTableModel.setData(workingSettings.availableProviders);
    }

    private void removeAvailableProvider(int rowIndex) {
        AIProviderConfig config = availableProvidersTableModel.getProviderConfig(rowIndex);
        if (config == null) {
            return;
        }
        String provider = config.providerType != null ? config.providerType.getDisplayName() : AICommonBundle.message("settings.available" +
                                                                                                                      ".providers.unknown");
        String model = config.modelName != null ? config.modelName : "";
        int result = JOptionPane.showConfirmDialog(mainPanel,
                                                   AICommonBundle.message("settings.available.providers.delete.confirm", provider, model),
                                                   AICommonBundle.message("settings.available.providers.delete.title"),
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            removeAvailableProvider(config.credentialId);
        }
    }

    private void clearAllAvailableProviders() {
        if (workingSettings.availableProviders.isEmpty()) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(mainPanel,
                                                   AICommonBundle.message("settings.available.providers.clear.confirm",
                                                                          workingSettings.availableProviders.size()),
                                                   AICommonBundle.message("settings.available.providers.clear.title"),
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            workingSettings.clearAvailableProviders();
            availableProvidersTableModel.setData(List.of());
        }
    }

    private void updateTestButtonState() {
        if (configurationVerified != null && configurationVerified) {
            testConnectionButton.setIcon(createStatusDotIcon(new JBColor(new Color(76, 175, 80), new Color(76, 175, 80))));
        } else {
            testConnectionButton.setIcon(createStatusDotIcon(new JBColor(new Color(244, 67, 54), new Color(244, 67, 54))));
        }
    }

    private void updateRefreshButtonState() {
        if (refreshModelsButton == null) {
            return;
        }
        if (refreshModelsSuccess == null) {
            refreshModelsButton.setIcon(createStatusDotIcon(new JBColor(new Color(255, 193, 7), new Color(255, 193, 7))));
        } else if (refreshModelsSuccess) {
            refreshModelsButton.setIcon(createStatusDotIcon(new JBColor(new Color(76, 175, 80), new Color(76, 175, 80))));
        } else {
            refreshModelsButton.setIcon(createStatusDotIcon(new JBColor(new Color(244, 67, 54), new Color(244, 67, 54))));
        }
    }

    private void updateApiKeyEnabled(@NotNull AIProviderType providerType) {
        apiKeyField.setEnabled(providerType.requiresApiKey());
        if (!providerType.requiresApiKey()) {
            apiKeyField.setText("");
        }
    }

    private void updateBaseUrlEditable(@NotNull AIProviderType providerType) {
        baseUrlField.setEditable(providerType.isBaseUrlEditable());
        if (!providerType.isBaseUrlEditable()) {
            baseUrlField.setText(providerType.getDefaultBaseUrl());
        }
    }

    private Icon createStatusDotIcon(Color color) {
        int size = 6;
        BufferedImage image = ImageUtil.createImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);
        g2d.dispose();
        return new ImageIcon(image);
    }

    private AIProviderType resolveSelectedProviderType() {
        String displayName = (String) providerComboBox.getSelectedItem();
        AIProviderType type = displayName != null ? AIProviderType.fromDisplayName(displayName) : null;
        return type != null ? type : AIProviderType.QIANWEN;
    }

    @NotNull
    private static String normalizeBaseUrl(@Nullable String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static class ProviderListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String displayName) {
                label.setText(displayName);
            }
            return label;
        }
    }

    private static class ProviderTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel label && value instanceof AIProviderConfig config) {
                String displayName = config.providerType != null ? config.providerType.getDisplayName() : AICommonBundle.message(
                    "settings.available.providers.unknown");
                label.setText(displayName);
            }
            return component;
        }
    }

    private static class AvailableProvidersTableModel extends AbstractTableModel {
        private final String[] columnNames = {
            AICommonBundle.message("settings.available.providers.column.provider"),
            AICommonBundle.message("settings.available.providers.column.model"),
            AICommonBundle.message("settings.available.providers.column.remark")
        };
        private final List<AIProviderConfig> data = new ArrayList<>();

        public void setData(List<AIProviderConfig> configs) {
            data.clear();
            configs.forEach(config -> data.add(config.copy()));
            fireTableDataChanged();
        }

        public List<AIProviderConfig> getData() {
            List<AIProviderConfig> copy = new ArrayList<>();
            data.forEach(config -> copy.add(config.copy()));
            return copy;
        }

        public AIProviderConfig getProviderConfig(int index) {
            if (index >= 0 && index < data.size()) {
                return data.get(index);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AIProviderConfig config = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> config;
                case 1 -> config.modelName != null ? config.modelName : "";
                case 2 -> config.remark != null ? config.remark : "";
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 2 && rowIndex >= 0 && rowIndex < data.size()) {
                data.get(rowIndex).remark = aValue != null ? aValue.toString() : "";
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}
