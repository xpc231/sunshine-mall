package com.xpcjsu.sunshinemall.framework.base.exception.xpc;

import java.util.HashMap;
import java.util.Map;

//该类与BaseExceptionyi一样，只是用重敲代码
public class xpcBaseException extends RuntimeException {

    private final String errorCode;

    private final String details;

    private final Map<String, Object> context;

    public xpcBaseException(String message) {
        this(null, message, null, null, new HashMap<>());
    }

    public xpcBaseException(String errorCode, String message) {
        this(errorCode, message, null, null, new HashMap<>());
    }

    public xpcBaseException(String message, Throwable cause) {
        this(null, message, null, cause, new HashMap<>());
    }

    public xpcBaseException(String errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause, new HashMap<>());
    }

    public xpcBaseException(String errorCode, String message, String details) {
        this(errorCode, message, details, null, new HashMap<>());
    }

    public xpcBaseException(String errorCode, String message, String details, Throwable cause) {
        this(errorCode, message, details, cause, new HashMap<>());
    }

    public xpcBaseException(String errorCode, String message, String details, Throwable cause, Map<String, Object> context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }

    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }

    public xpcBaseException addContext(Map<String, Object> contextData) {
        if(contextData != null) {
            this.context.putAll(contextData);
        }
        return this;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());

        if(errorCode != null) {
            sb.append(" [").append(errorCode).append("]");
        }

        if (getMessage() != null) {
            sb.append(": ").append(getMessage());
        }

        if (details != null) {
            sb.append(" (Details: ").append(details).append(")");
        }

        if (!context.isEmpty()) {
            sb.append(" (Context: ").append(context).append(")");
        }

        return sb.toString();
    }
}
