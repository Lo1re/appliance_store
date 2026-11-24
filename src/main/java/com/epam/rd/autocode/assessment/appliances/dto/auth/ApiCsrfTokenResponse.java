package com.epam.rd.autocode.assessment.appliances.dto.auth;

// CSRF form/header value is separate from the HttpOnly access credential.
public record ApiCsrfTokenResponse(String headerName, String parameterName, String token) {
    @Override
    public String toString() {
        return "ApiCsrfTokenResponse[redacted]";
    }
}
