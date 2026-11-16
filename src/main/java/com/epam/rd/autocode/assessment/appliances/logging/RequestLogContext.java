package com.epam.rd.autocode.assessment.appliances.logging;

public final class RequestLogContext {
    public static final String MDC_KEY = "requestId";
    public static final String HEADER = "X-Request-ID";
    public static final String ATTRIBUTE = RequestLogContext.class.getName() + ".id";

    private RequestLogContext() {
    }
}
