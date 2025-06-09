package com.codebridge.core.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for HTTP request operations.
 */
public class RequestUtils {

    private static final Logger logger = LoggerFactory.getLogger(RequestUtils.class);

    // List of headers that might contain the client IP address
    private static final List<String> IP_HEADERS = Arrays.asList(
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
    );

    // Private constructor to prevent instantiation
    private RequestUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Extracts the client IP address from the request.
     * Handles various proxy headers and forwarding scenarios.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // Try each header in order
        for (String header : IP_HEADERS) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                // X-Forwarded-For may contain multiple IPs, the first one is the client
                String ip = ipList.split(",")[0].trim();
                if (isValidIp(ip)) {
                    return ip;
                }
            }
        }

        // If no headers contain the IP, use the remote address
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null) ? remoteAddr : "unknown";
    }

    /**
     * Gets the user agent from the request.
     *
     * @param request the HTTP request
     * @return the user agent string
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        return (userAgent != null) ? userAgent : "unknown";
    }

    /**
     * Gets the request method from the request.
     *
     * @param request the HTTP request
     * @return the request method
     */
    public static String getRequestMethod(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String method = request.getMethod();
        return (method != null) ? method : "unknown";
    }

    /**
     * Gets the request URI from the request.
     *
     * @param request the HTTP request
     * @return the request URI
     */
    public static String getRequestUri(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String uri = request.getRequestURI();
        return (uri != null) ? uri : "unknown";
    }

    /**
     * Checks if an IP address is valid.
     *
     * @param ip the IP address to check
     * @return true if the IP is valid, false otherwise
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Simple validation for IPv4 and IPv6
        // For more comprehensive validation, consider using a library
        return !ip.equalsIgnoreCase("unknown") &&
               !ip.equalsIgnoreCase("0:0:0:0:0:0:0:1") &&
               !ip.equalsIgnoreCase("localhost") &&
               !ip.equals("127.0.0.1");
    }
}

