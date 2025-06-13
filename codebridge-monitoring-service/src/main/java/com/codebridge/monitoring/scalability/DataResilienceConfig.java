package com.codebridge.monitoring.scalability.config;

import com.codebridge.monitoring.scalability.resilience.BackupService;
import com.codebridge.monitoring.scalability.resilience.DataPartitioningService;
import com.codebridge.monitoring.scalability.resilience.ReplicationService;
import com.codebridge.monitoring.scalability.resilience.impl.DefaultBackupService;
import com.codebridge.monitoring.scalability.resilience.impl.DefaultDataPartitioningService;
import com.codebridge.monitoring.scalability.resilience.impl.DefaultReplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration for data resilience components.
 */
@Configuration
@RequiredArgsConstructor
public class DataResilienceConfig {

    private final JdbcTemplate jdbcTemplate;

    @Value("${codebridge.scalability.data-resilience.replication.enabled}")
    private boolean replicationEnabled;

    @Value("${codebridge.scalability.data-resilience.replication.read-from-replicas}")
    private boolean readFromReplicas;

    @Value("${codebridge.scalability.data-resilience.replication.consistency-level}")
    private String consistencyLevel;

    @Value("${codebridge.scalability.data-resilience.backup.enabled}")
    private boolean backupEnabled;

    @Value("${codebridge.scalability.data-resilience.backup.schedule}")
    private String backupSchedule;

    @Value("${codebridge.scalability.data-resilience.backup.retention-days}")
    private int backupRetentionDays;

    @Value("${codebridge.scalability.data-resilience.backup.verify}")
    private boolean verifyBackups;

    @Value("${codebridge.scalability.data-resilience.partitioning.enabled}")
    private boolean partitioningEnabled;

    @Value("${codebridge.scalability.data-resilience.partitioning.strategy}")
    private String partitioningStrategy;

    @Value("${codebridge.scalability.data-resilience.partitioning.shard-count}")
    private int shardCount;

    /**
     * Creates a replication service.
     *
     * @return the replication service
     */
    @Bean
    public ReplicationService replicationService() {
        return new DefaultReplicationService(
                replicationEnabled,
                readFromReplicas,
                consistencyLevel,
                jdbcTemplate
        );
    }

    /**
     * Creates a backup service.
     *
     * @return the backup service
     */
    @Bean
    public BackupService backupService() {
        return new DefaultBackupService(
                backupEnabled,
                backupSchedule,
                backupRetentionDays,
                verifyBackups,
                jdbcTemplate
        );
    }

    /**
     * Creates a data partitioning service.
     *
     * @return the data partitioning service
     */
    @Bean
    public DataPartitioningService dataPartitioningService() {
        return new DefaultDataPartitioningService(
                partitioningEnabled,
                partitioningStrategy,
                shardCount,
                jdbcTemplate
        );
    }
}

