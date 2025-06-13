package com.codebridge.monitoring.performance.service;

import com.codebridge.monitoring.performance.model.PerformanceMetric;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for storing and querying time-series data using InfluxDB.
 */
@Service
@Slf4j
public class TimeSeriesService {

    @Value("${performance.metrics.influxdb.url}")
    private String influxDbUrl;

    @Value("${performance.metrics.influxdb.token}")
    private String influxDbToken;

    @Value("${performance.metrics.influxdb.org}")
    private String influxDbOrg;

    @Value("${performance.metrics.influxdb.bucket}")
    private String influxDbBucket;

    private InfluxDB influxDB;

    /**
     * Initialize the InfluxDB connection.
     */
    @PostConstruct
    public void init() {
        try {
            influxDB = InfluxDBFactory.connect(influxDbUrl, influxDbToken, "");
            influxDB.setDatabase(influxDbBucket);
            influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);
            log.info("Connected to InfluxDB at {}", influxDbUrl);
        } catch (Exception e) {
            log.error("Failed to connect to InfluxDB: {}", e.getMessage(), e);
        }
    }

    /**
     * Close the InfluxDB connection.
     */
    @PreDestroy
    public void close() {
        if (influxDB != null) {
            influxDB.close();
            log.info("Closed InfluxDB connection");
        }
    }

    /**
     * Store metrics in InfluxDB.
     *
     * @param metrics the metrics to store
     */
    public void storeMetrics(List<PerformanceMetric> metrics) {
        if (influxDB == null) {
            log.warn("InfluxDB connection not available, skipping metric storage");
            return;
        }

        try {
            for (PerformanceMetric metric : metrics) {
                Point.Builder pointBuilder = Point.measurement(metric.getServiceName())
                        .time(metric.getTimestamp().toEpochMilli(), TimeUnit.MILLISECONDS)
                        .tag("metric_name", metric.getMetricName())
                        .tag("metric_type", metric.getMetricType().name())
                        .addField("value", metric.getValue());

                // Add custom tags
                for (Map.Entry<String, String> tag : metric.getTags().entrySet()) {
                    pointBuilder.tag(tag.getKey(), tag.getValue());
                }

                influxDB.write(pointBuilder.build());
            }
            log.debug("Stored {} metrics in InfluxDB", metrics.size());
        } catch (Exception e) {
            log.error("Error storing metrics in InfluxDB: {}", e.getMessage(), e);
        }
    }

    /**
     * Query aggregated metric from InfluxDB.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @param aggregation the aggregation function (mean, min, max, sum, count)
     * @return the aggregated metric value
     */
    public double queryAggregatedMetric(String serviceName, String metricName, 
                                       Instant startTime, Instant endTime, String aggregation) {
        if (influxDB == null) {
            log.warn("InfluxDB connection not available, returning default value");
            return 0.0;
        }

        try {
            String queryString = String.format(
                    "SELECT %s(value) FROM %s WHERE metric_name = '%s' AND time >= %d AND time <= %d",
                    aggregation, serviceName, metricName, 
                    startTime.toEpochMilli() * 1000000, endTime.toEpochMilli() * 1000000);

            QueryResult queryResult = influxDB.query(new Query(queryString, influxDbBucket));
            
            if (queryResult.hasError()) {
                log.error("Error querying InfluxDB: {}", queryResult.getError());
                return 0.0;
            }

            List<QueryResult.Result> results = queryResult.getResults();
            if (results == null || results.isEmpty() || results.get(0).getSeries() == null || results.get(0).getSeries().isEmpty()) {
                return 0.0;
            }

            QueryResult.Series series = results.get(0).getSeries().get(0);
            if (series.getValues() == null || series.getValues().isEmpty() || series.getValues().get(0).get(1) == null) {
                return 0.0;
            }

            return ((Number) series.getValues().get(0).get(1)).doubleValue();
        } catch (Exception e) {
            log.error("Error querying aggregated metric from InfluxDB: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    /**
     * Query time-series data from InfluxDB.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @param interval the interval for grouping (e.g., "1m", "5m", "1h")
     * @return the query result
     */
    public QueryResult queryTimeSeriesData(String serviceName, String metricName, 
                                          Instant startTime, Instant endTime, String interval) {
        if (influxDB == null) {
            log.warn("InfluxDB connection not available, returning empty result");
            return new QueryResult();
        }

        try {
            String queryString = String.format(
                    "SELECT mean(value) FROM %s WHERE metric_name = '%s' AND time >= %d AND time <= %d GROUP BY time(%s)",
                    serviceName, metricName, 
                    startTime.toEpochMilli() * 1000000, endTime.toEpochMilli() * 1000000, interval);

            return influxDB.query(new Query(queryString, influxDbBucket));
        } catch (Exception e) {
            log.error("Error querying time-series data from InfluxDB: {}", e.getMessage(), e);
            return new QueryResult();
        }
    }
}

