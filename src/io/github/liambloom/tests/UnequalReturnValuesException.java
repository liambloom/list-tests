package io.github.liambloom.tests;

public class UnequalReturnValuesException extends IllegalStateException {
    final String message;
    private static final long serialVersionUID = 1L;

    public UnequalReturnValuesException(String message, Object... formatArgs) {
        this.message = String.format(message, formatArgs);
    }

    public String toString() {
        return message;
    }
}