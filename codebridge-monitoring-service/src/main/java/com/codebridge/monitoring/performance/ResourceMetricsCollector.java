package com.codebridge.monitoring.performance.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Collector for system resource metrics.
 * This collector captures CPU, memory, disk, and thread utilization metrics.
 */
@Component
@Slf4j
public class ResourceMetricsCollector {

    private static final String SERVICE_NAME = "resource-metrics";
    
    private final PerformanceMetricsCollector metricsCollector;
    private final OperatingSystemMXBean osMXBean;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    
    @Value("${services.resource.metrics-enabled:true}")
    private boolean resourceMetricsEnabled;
    
    @Value("${services.resource.cpu-threshold:80}")
    private double cpuThreshold;
    
    @Value("${services.resource.memory-threshold:80}")
    private double memoryThreshold;
    
    @Value("${services.resource.disk-threshold:90}")
    private double diskThreshold;

    @Autowired
    public ResourceMetricsCollector(PerformanceMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * Scheduled task to collect system resource metrics.
     */
    @Scheduled(fixedDelayString = "${performance.metrics.collection.interval:15000}")
    public void collectResourceMetrics() {
        if (!resourceMetricsEnabled) {
            return;
        }
        
        collectCpuMetrics();
        collectMemoryMetrics();
        collectDiskMetrics();
        collectThreadMetrics();
    }
    
    /**
     * Collect CPU metrics.
     */
    private void collectCpuMetrics() {
        try {
            // Get system CPU load
            double systemCpuLoad = getSystemCpuLoad();
            
            // Get process CPU load
            double processCpuLoad = getProcessCpuLoad();
            
            // Get available processors
            int availableProcessors = osMXBean.getAvailableProcessors();
            
            Map<String, String> tags = new HashMap<>();
            
            // Record CPU metrics
            metricsCollector.recordGauge(SERVICE_NAME, "cpu.system.load", systemCpuLoad * 100, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "cpu.process.load", processCpuLoad * 100, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "cpu.available.processors", availableProcessors, tags);
            
            // Check if CPU usage exceeds threshold
            if (systemCpuLoad * 100 >= cpuThreshold) {
                metricsCollector.incrementCounter(SERVICE_NAME, "cpu.threshold.exceeded", tags);
            }
        } catch (Exception e) {
            log.error("Error collecting CPU metrics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Collect memory metrics.
     */
    private void collectMemoryMetrics() {
        try {
            // Get heap memory usage
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            double heapUtilization = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
            
            // Get non-heap memory usage
            long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            
            // Get total physical memory (if available)
            long totalPhysicalMemory = getTotalPhysicalMemory();
            long freePhysicalMemory = getFreePhysicalMemory();
            double physicalMemoryUtilization = totalPhysicalMemory > 0 ? 
                    (double) (totalPhysicalMemory - freePhysicalMemory) / totalPhysicalMemory * 100 : 0;
            
            Map<String, String> tags = new HashMap<>();
            
            // Record memory metrics
            metricsCollector.recordGauge(SERVICE_NAME, "memory.heap.used", heapUsed, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "memory.heap.max", heapMax, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "memory.heap.utilization", heapUtilization, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "memory.non-heap.used", nonHeapUsed, tags);
            
            if (totalPhysicalMemory > 0) {
                metricsCollector.recordGauge(SERVICE_NAME, "memory.physical.total", totalPhysicalMemory, tags);
                metricsCollector.recordGauge(SERVICE_NAME, "memory.physical.free", freePhysicalMemory, tags);
                metricsCollector.recordGauge(SERVICE_NAME, "memory.physical.utilization", physicalMemoryUtilization, tags);
            }
            
            // Check if memory usage exceeds threshold
            if (heapUtilization >= memoryThreshold || physicalMemoryUtilization >= memoryThreshold) {
                metricsCollector.incrementCounter(SERVICE_NAME, "memory.threshold.exceeded", tags);
            }
        } catch (Exception e) {
            log.error("Error collecting memory metrics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Collect disk metrics.
     */
    private void collectDiskMetrics() {
        try {
            // Get disk usage for root directory
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long usableSpace = root.getUsableSpace();
            double diskUtilization = totalSpace > 0 ? 
                    (double) (totalSpace - usableSpace) / totalSpace * 100 : 0;
            
            Map<String, String> tags = new HashMap<>();
            tags.put("path", "/");
            
            // Record disk metrics
            metricsCollector.recordGauge(SERVICE_NAME, "disk.total", totalSpace, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "disk.usable", usableSpace, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "disk.utilization", diskUtilization, tags);
            
            // Check if disk usage exceeds threshold
            if (diskUtilization >= diskThreshold) {
                metricsCollector.incrementCounter(SERVICE_NAME, "disk.threshold.exceeded", tags);
            }
        } catch (Exception e) {
            log.error("Error collecting disk metrics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Collect thread metrics.
     */
    private void collectThreadMetrics() {
        try {
            // Get thread counts
            int threadCount = threadMXBean.getThreadCount();
            int peakThreadCount = threadMXBean.getPeakThreadCount();
            long totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();
            int daemonThreadCount = threadMXBean.getDaemonThreadCount();
            
            Map<String, String> tags = new HashMap<>();
            
            // Record thread metrics
            metricsCollector.recordGauge(SERVICE_NAME, "thread.count", threadCount, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "thread.peak", peakThreadCount, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "thread.total.started", totalStartedThreadCount, tags);
            metricsCollector.recordGauge(SERVICE_NAME, "thread.daemon.count", daemonThreadCount, tags);
        } catch (Exception e) {
            log.error("Error collecting thread metrics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get the system CPU load.
     *
     * @return the system CPU load as a value between 0.0 and 1.0
     */
    private double getSystemCpuLoad() {
        try {
            // Try to access com.sun.management.OperatingSystemMXBean.getSystemCpuLoad() using reflection
            java.lang.reflect.Method method = osMXBean.getClass().getMethod("getSystemCpuLoad");
            method.setAccessible(true);
            return (double) method.invoke(osMXBean);
        } catch (Exception e) {
            // Fallback to getSystemLoadAverage() which might not be as accurate
            return osMXBean.getSystemLoadAverage() / osMXBean.getAvailableProcessors();
        }
    }
    
    /**
     * Get the process CPU load.
     *
     * @return the process CPU load as a value between 0.0 and 1.0
     */
    private double getProcessCpuLoad() {
        try {
            // Try to access com.sun.management.OperatingSystemMXBean.getProcessCpuLoad() using reflection
            java.lang.reflect.Method method = osMXBean.getClass().getMethod("getProcessCpuLoad");
            method.setAccessible(true);
            return (double) method.invoke(osMXBean);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Get the total physical memory.
     *
     * @return the total physical memory in bytes
     */
    private long getTotalPhysicalMemory() {
        try {
            // Try to access com.sun.management.OperatingSystemMXBean.getTotalPhysicalMemorySize() using reflection
            java.lang.reflect.Method method = osMXBean.getClass().getMethod("getTotalPhysicalMemorySize");
            method.setAccessible(true);
            return (long) method.invoke(osMXBean);
        } catch (Exception e) {
            return 0L;
        }
    }
    
    /**
     * Get the free physical memory.
     *
     * @return the free physical memory in bytes
     */
    private long getFreePhysicalMemory() {
        try {
            // Try to access com.sun.management.OperatingSystemMXBean.getFreePhysicalMemorySize() using reflection
            java.lang.reflect.Method method = osMXBean.getClass().getMethod("getFreePhysicalMemorySize");
            method.setAccessible(true);
            return (long) method.invoke(osMXBean);
        } catch (Exception e) {
            return 0L;
        }
    }
}

