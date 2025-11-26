package dev.dong4j.zeka.stack.idea.plugin.archiver.core;

/**
 * 压缩包编辑相关异常
 *
 * @author dong4j
 * @since 0.2.0
 */
public class EditableArchiveException extends Exception {
    public EditableArchiveException(String message) {
        super(message);
    }

    public EditableArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}

