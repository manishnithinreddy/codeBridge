package com.codebridge.core.model;

import com.codebridge.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service extends BaseEntity {

    public enum ServiceType {
        GIT_PROVIDER,
        CI_CD,
        ISSUE_TRACKER,
        DOCUMENTATION,
        MONITORING,
        MESSAGING,
        STORAGE,
        DATABASE,
        CUSTOM
    }

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "documentation_url")
    private String documentationUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType type;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "requires_authentication", nullable = false)
    private boolean requiresAuthentication;

    @Column(name = "auth_type")
    private String authType;

    @Column(name = "config_schema", columnDefinition = "TEXT")
    private String configSchema;

    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    private Set<TeamService> teamServices = new HashSet<>();
}

