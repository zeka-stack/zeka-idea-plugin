package dev.dong4j.zeka.stack.idea.plugin.archiver.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverBundle;

/**
 * 设置面板。
 */
final class ArchiverSettingsPanel {
    private final JBCheckBox enableBrowser = new JBCheckBox(ArchiverBundle.message("settings.browser.enable"));
    private final JBCheckBox enableEditable = new JBCheckBox(ArchiverBundle.message("settings.editable.enable"));
    private final JBCheckBox enableBackup = new JBCheckBox(ArchiverBundle.message("settings.backup.enable"));
    private final JBCheckBox showBadge = new JBCheckBox(ArchiverBundle.message("settings.badge.enable"));
    private final JBTextField maxSizeField = new JBTextField();
    private final ComboBox<Integer> delayCombo = new ComboBox<>(new Integer[] {0, 200, 500, 1000});

    private final JPanel panel;

    ArchiverSettingsPanel() {
        panel = FormBuilder.createFormBuilder()
            .addComponent(enableBrowser)
            .addComponent(enableEditable)
            .addComponent(enableBackup)
            .addComponent(showBadge)
            .addLabeledComponent(new JBLabel(ArchiverBundle.message("settings.max.size")), maxSizeField, 5, false)
            .addLabeledComponent(new JBLabel(ArchiverBundle.message("settings.batch.delay")), delayCombo, 5, false)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        panel.setBorder(JBUI.Borders.empty(10));
    }

    JComponent getComponent() {
        return panel;
    }

    void reset(@NotNull ArchiverSettingsState state) {
        enableBrowser.setSelected(state.enableArchiveBrowser);
        enableEditable.setSelected(state.enableEditableMode);
        enableBackup.setSelected(state.enableAutoBackup);
        showBadge.setSelected(state.showEditableBadge);
        maxSizeField.setText(String.valueOf(state.maxEditableFileSizeMb));
        if (((javax.swing.DefaultComboBoxModel<Integer>) delayCombo.getModel()).getIndexOf(state.batchSaveDelayMillis) == -1) {
            delayCombo.addItem(state.batchSaveDelayMillis);
        }
        delayCombo.setSelectedItem(state.batchSaveDelayMillis);
    }

    ArchiverSettingsState collectState() {
        ArchiverSettingsState state = new ArchiverSettingsState();
        apply(state);
        return state;
    }

    void apply(@NotNull ArchiverSettingsState state) {
        state.enableArchiveBrowser = enableBrowser.isSelected();
        state.enableEditableMode = enableEditable.isSelected();
        state.enableAutoBackup = enableBackup.isSelected();
        state.showEditableBadge = showBadge.isSelected();
        state.maxEditableFileSizeMb = parseInt(maxSizeField.getText(), state.maxEditableFileSizeMb);
        state.batchSaveDelayMillis = (Integer) delayCombo.getSelectedItem();
    }

    private int parseInt(String text, int fallback) {
        try {
            int value = Integer.parseInt(text);
            return Math.max(1, value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}

