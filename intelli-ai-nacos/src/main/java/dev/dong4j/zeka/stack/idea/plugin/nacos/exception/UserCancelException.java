package dev.dong4j.zeka.stack.idea.plugin.nacos.exception;

/**
 * 用户取消操作异常
 * 当用户主动取消某个操作时抛出此异常
 *
 * @author dong4j
 * @since 1.0.0
 */
public class UserCancelException extends RuntimeException {

    public UserCancelException() {
    }

    public UserCancelException(String msg) {
        super(msg);
    }
}
