package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class representing GitLab Time Statistics.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabTimeStats {

    @JsonProperty("time_estimate")
    private Integer timeEstimate;
    
    @JsonProperty("total_time_spent")
    private Integer totalTimeSpent;
    
    @JsonProperty("human_time_estimate")
    private String humanTimeEstimate;
    
    @JsonProperty("human_total_time_spent")
    private String humanTotalTimeSpent;

    // Getters and setters
    public Integer getTimeEstimate() {
        return timeEstimate;
    }

    public void setTimeEstimate(Integer timeEstimate) {
        this.timeEstimate = timeEstimate;
    }

    public Integer getTotalTimeSpent() {
        return totalTimeSpent;
    }

    public void setTotalTimeSpent(Integer totalTimeSpent) {
        this.totalTimeSpent = totalTimeSpent;
    }

    public String getHumanTimeEstimate() {
        return humanTimeEstimate;
    }

    public void setHumanTimeEstimate(String humanTimeEstimate) {
        this.humanTimeEstimate = humanTimeEstimate;
    }

    public String getHumanTotalTimeSpent() {
        return humanTotalTimeSpent;
    }

    public void setHumanTotalTimeSpent(String humanTotalTimeSpent) {
        this.humanTotalTimeSpent = humanTotalTimeSpent;
    }
}

