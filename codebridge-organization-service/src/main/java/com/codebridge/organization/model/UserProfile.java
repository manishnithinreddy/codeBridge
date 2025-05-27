package com.codebridge.organization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user profile.
 * Contains extended information about a user.
 */
@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private UUID id;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column
    private String bio;

    @Column(name = "job_title")
    private String jobTitle;

    @Column
    private String department;

    @Column
    private String location;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "language_preference")
    private String languagePreference;

    @Column(name = "theme_preference")
    private String themePreference;

    @Column(name = "notification_preferences", columnDefinition = "TEXT")
    private String notificationPreferences;

    @Column(name = "feature_flags", columnDefinition = "TEXT")
    private String featureFlags;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

