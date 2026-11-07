package com.interview.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    static final String HEADER = "X-Admin-Key";

    private final InterviewProperties properties;

    public AdminAccessInterceptor(InterviewProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!requiresAdmin(request.getMethod())) {
            return true;
        }
        String provided = request.getHeader(HEADER);
        if (provided != null && provided.length() > 256) {
            writeUnauthorized(response);
            return false;
        }
        if (!keysMatch(provided)) {
            writeUnauthorized(response);
            return false;
        }
        return true;
    }

    private boolean requiresAdmin(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method);
    }

    private boolean keysMatch(String provided) {
        String expected = properties.getAdminKey();
        if (expected == null || expected.isBlank() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"status":401,"error":"Unauthorized","message":"Admin access required. Open Admin and enter the admin key.","fields":null}
                """);
    }

}
