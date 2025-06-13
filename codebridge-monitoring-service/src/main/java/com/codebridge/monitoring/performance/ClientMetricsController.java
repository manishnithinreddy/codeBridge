package com.codebridge.monitoring.performance.controller;

import com.codebridge.monitoring.performance.collector.PerformanceMetricsCollector;
import com.codebridge.monitoring.performance.dto.ClientMetricsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Controller for collecting client-side metrics.
 */
@RestController
@RequestMapping("/api/metrics/client")
@Slf4j
public class ClientMetricsController {

    private static final String SERVICE_NAME = "client-metrics";
    private final PerformanceMetricsCollector metricsCollector;
    private final Random random = new Random();
    
    @Value("${performance.client-metrics.sampling-rate:0.1}")
    private double samplingRate;

    @Autowired
    public ClientMetricsController(PerformanceMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /**
     * Collect client-side metrics.
     *
     * @param metrics the client metrics
     * @return the response entity
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> collectMetrics(@Valid @RequestBody ClientMetricsDto metrics) {
        // Apply sampling to reduce the volume of metrics
        if (random.nextDouble() > samplingRate) {
            return ResponseEntity.ok(Map.of("status", "sampled"));
        }
        
        try {
            // Process and store the metrics
            processClientMetrics(metrics);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Error processing client metrics: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * Process and store client metrics.
     *
     * @param metrics the client metrics
     */
    private void processClientMetrics(ClientMetricsDto metrics) {
        Map<String, String> tags = new HashMap<>();
        tags.put("page", metrics.getPage());
        tags.put("browser", metrics.getBrowser());
        tags.put("browser_version", metrics.getBrowserVersion());
        tags.put("os", metrics.getOs());
        tags.put("device_type", metrics.getDeviceType());
        tags.put("connection_type", metrics.getConnectionType());
        
        // Record page load metrics
        if (metrics.getPageLoadTime() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "page.load.time", metrics.getPageLoadTime(), tags);
        }
        
        if (metrics.getDomContentLoadedTime() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "dom.content.loaded.time", metrics.getDomContentLoadedTime(), tags);
        }
        
        if (metrics.getFirstContentfulPaint() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "first.contentful.paint", metrics.getFirstContentfulPaint(), tags);
        }
        
        if (metrics.getLargestContentfulPaint() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "largest.contentful.paint", metrics.getLargestContentfulPaint(), tags);
        }
        
        if (metrics.getFirstInputDelay() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "first.input.delay", metrics.getFirstInputDelay(), tags);
        }
        
        if (metrics.getCumulativeLayoutShift() != null) {
            metricsCollector.recordGauge(SERVICE_NAME, "cumulative.layout.shift", metrics.getCumulativeLayoutShift(), tags);
        }
        
        // Record resource metrics
        if (metrics.getResourceLoadTime() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "resource.load.time", metrics.getResourceLoadTime(), tags);
        }
        
        if (metrics.getResourceCount() != null) {
            metricsCollector.recordGauge(SERVICE_NAME, "resource.count", metrics.getResourceCount(), tags);
        }
        
        // Record network metrics
        if (metrics.getNetworkRequestTime() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "network.request.time", metrics.getNetworkRequestTime(), tags);
        }
        
        if (metrics.getNetworkResponseTime() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "network.response.time", metrics.getNetworkResponseTime(), tags);
        }
        
        if (metrics.getNetworkLatency() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "network.latency", metrics.getNetworkLatency(), tags);
        }
        
        // Record user experience metrics
        if (metrics.getTimeToInteractive() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "time.to.interactive", metrics.getTimeToInteractive(), tags);
        }
        
        if (metrics.getUserTiming() != null) {
            metricsCollector.recordTimer(SERVICE_NAME, "user.timing", metrics.getUserTiming(), tags);
        }
        
        // Record error metrics
        if (metrics.getJsErrorCount() != null) {
            metricsCollector.recordGauge(SERVICE_NAME, "js.error.count", metrics.getJsErrorCount(), tags);
        }
        
        if (metrics.getApiErrorCount() != null) {
            metricsCollector.recordGauge(SERVICE_NAME, "api.error.count", metrics.getApiErrorCount(), tags);
        }
        
        // Record custom metrics
        if (metrics.getCustomMetrics() != null) {
            metrics.getCustomMetrics().forEach((key, value) -> {
                metricsCollector.recordGauge(SERVICE_NAME, "custom." + key, value, tags);
            });
        }
    }
}

