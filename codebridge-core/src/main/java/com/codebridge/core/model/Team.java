package com.codebridge.core.model;

import com.codebridge.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Team extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(name = "is_personal")
    private boolean isPersonal;

    @Column(name = "owner_user_id")
    private String ownerUserId;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private Set<UserTeamRole> userTeamRoles = new HashSet<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private Set<TeamService> teamServices = new HashSet<>();
}

