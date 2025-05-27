package com.codebridge.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a user session.
 */
public class SessionCreateRequest {

    @NotBlank(message = "IP address is required")
    private String ipAddress;

    @NotBlank(message = "User agent is required")
    private String userAgent;

    private String deviceInfo;

    private String geoLocation;

    private String refreshToken;

    public SessionCreateRequest() {
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

