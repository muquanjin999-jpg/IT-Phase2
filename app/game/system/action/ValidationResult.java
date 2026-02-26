package game.system.action;

public final class ValidationResult {
    private static final ValidationResult OK = new ValidationResult(true, "OK");

    private final boolean ok;
    private final String message;

    private ValidationResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message == null ? "" : message;
    }

    public static ValidationResult ok() {
        return OK;
    }

    public static ValidationResult fail(String message) {
        return new ValidationResult(false, message);
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return ok ? "ValidationResult{OK}" : "ValidationResult{FAIL: " + message + "}";
    }
}
