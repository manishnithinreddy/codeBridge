package com.codebridge.core.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Utility class for HTTP request operations.
 */
public class RequestUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    /**
     * Gets the client IP address from the request.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        for (String header : IP_HEADERS) {
            String ipList = request.getHeader(header);
            if (StringUtils.hasText(ipList) && !"unknown".equalsIgnoreCase(ipList)) {
                // X-Forwarded-For can contain multiple IPs, the first one is the client
                String ip = ipList.split(",")[0].trim();
                if (StringUtils.hasText(ip)) {
                    return ip;
                }
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Gets the user agent from the request.
     *
     * @param request the HTTP request
     * @return the user agent
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        return StringUtils.hasText(userAgent) ? userAgent : "unknown";
    }

    /**
     * Gets the request URL including query string.
     *
     * @param request the HTTP request
     * @return the request URL
     */
    public static String getRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String queryString = request.getQueryString();
        if (StringUtils.hasText(queryString)) {
            return request.getRequestURL().append("?").append(queryString).toString();
        }
        return request.getRequestURL().toString();
    }

    /**
     * Gets the request method.
     *
     * @param request the HTTP request
     * @return the request method
     */
    public static String getRequestMethod(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        return request.getMethod();
    }
}

