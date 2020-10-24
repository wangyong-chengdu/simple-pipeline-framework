package cd.wangyong.simple_pipeline;

/**
 * pipelne值对象
 * @author andy
 * @since 2020/10/23
 */
public class PipeValueObj {
    private final boolean success;
    private final Object value;
    private final String errorMsg;
    private final Throwable throwable;

    private PipeValueObj(boolean success, Object value, String errorMsg, Throwable throwable) {
        this.success = success;
        this.value = value;
        this.errorMsg = errorMsg;
        this.throwable = throwable;
    }

    public static PipeValueObj success(Object result) {
        return new PipeValueObj(true, result, null, null);
    }

    public static PipeValueObj fail(Throwable throwable) {
        return new PipeValueObj(false, null, throwable.getMessage(), throwable);
    }

    public static PipeValueObj fail(String errorMsg, Throwable throwable) {
        return new PipeValueObj(false, null, errorMsg, throwable);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getValue() {
        return value;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
