package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Model class representing a GitLab Merge Request Approval Rule.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabApprovalRule {

    private Integer id;
    private String name;
    
    @JsonProperty("rule_type")
    private String ruleType;
    
    @JsonProperty("approvals_required")
    private Integer approvalsRequired;
    
    @JsonProperty("eligible_approvers")
    private List<GitLabUser> eligibleApprovers;
    
    @JsonProperty("approved_by")
    private List<GitLabUser> approvedBy;
    
    @JsonProperty("contains_hidden_groups")
    private Boolean containsHiddenGroups;
    
    @JsonProperty("section")
    private String section;
    
    @JsonProperty("source_rule")
    private String sourceRule;

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getApprovalsRequired() {
        return approvalsRequired;
    }

    public void setApprovalsRequired(Integer approvalsRequired) {
        this.approvalsRequired = approvalsRequired;
    }

    public List<GitLabUser> getEligibleApprovers() {
        return eligibleApprovers;
    }

    public void setEligibleApprovers(List<GitLabUser> eligibleApprovers) {
        this.eligibleApprovers = eligibleApprovers;
    }

    public List<GitLabUser> getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(List<GitLabUser> approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Boolean getContainsHiddenGroups() {
        return containsHiddenGroups;
    }

    public void setContainsHiddenGroups(Boolean containsHiddenGroups) {
        this.containsHiddenGroups = containsHiddenGroups;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSourceRule() {
        return sourceRule;
    }

    public void setSourceRule(String sourceRule) {
        this.sourceRule = sourceRule;
    }
}

