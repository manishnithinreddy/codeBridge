package com.codebridge.core.model;

import com.codebridge.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_team_id")
    private Team parentTeam;

    @OneToMany(mappedBy = "parentTeam", fetch = FetchType.LAZY)
    private Set<Team> childTeams = new HashSet<>();

    @Column(nullable = false)
    private boolean active;

    @ManyToMany(mappedBy = "teams", fetch = FetchType.LAZY)
    private Set<User> members = new HashSet<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private Set<TeamService> teamServices = new HashSet<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private Set<UserTeamRole> teamRoles = new HashSet<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private Set<Token> tokens = new HashSet<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private Set<AuditLog> auditLogs = new HashSet<>();
}

