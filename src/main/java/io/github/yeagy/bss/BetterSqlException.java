package io.github.yeagy.bss;

public final class BetterSqlException extends RuntimeException {
    public BetterSqlException() {
        super();
    }

    public BetterSqlException(String message) {
        super(message);
    }

    public BetterSqlException(String message, Throwable cause) {
        super(message, cause);
    }

    public BetterSqlException(Throwable cause) {
        super(cause);
    }

    protected BetterSqlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
