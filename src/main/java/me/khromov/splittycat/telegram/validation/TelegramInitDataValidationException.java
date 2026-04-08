package me.khromov.splittycat.telegram.validation;

public class TelegramInitDataValidationException extends RuntimeException {

    public TelegramInitDataValidationException(String message) {
        super(message);
    }

    public TelegramInitDataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
