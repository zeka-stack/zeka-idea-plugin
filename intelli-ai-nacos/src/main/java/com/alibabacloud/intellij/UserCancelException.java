package com.alibabacloud.intellij;

public class UserCancelException extends RuntimeException {
    public UserCancelException() {
    }

    public UserCancelException(String msg) {
        super(msg);
    }
}
