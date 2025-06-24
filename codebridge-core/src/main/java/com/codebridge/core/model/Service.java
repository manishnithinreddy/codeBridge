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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Service extends BaseEntity {

    public enum ServiceType {
        GIT,
        CI_CD,
        ISSUE_TRACKER,
        DOCUMENTATION,
        MONITORING,
        ANALYTICS,
        DATABASE,
        STORAGE,
        MESSAGING,
        CUSTOM
    }

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    private Set<TeamService> teamServices = new HashSet<>();
}

