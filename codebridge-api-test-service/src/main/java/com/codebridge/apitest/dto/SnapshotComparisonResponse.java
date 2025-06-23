package com.codebridge.apitest.dto;

/**
 * DTO for snapshot comparison responses.
 */
public class SnapshotComparisonResponse {

    private Long testId;
    private Long resultId;
    private Boolean hasApprovedSnapshot;
    private Long snapshotId;
    private String snapshotName;
    private Boolean bodyMatches;
    private Boolean headersMatch;
    private Boolean statusMatches;
    private Boolean matches;

    // Getters and Setters

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public Boolean getHasApprovedSnapshot() {
        return hasApprovedSnapshot;
    }

    public void setHasApprovedSnapshot(Boolean hasApprovedSnapshot) {
        this.hasApprovedSnapshot = hasApprovedSnapshot;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public void setSnapshotName(String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public Boolean getBodyMatches() {
        return bodyMatches;
    }

    public void setBodyMatches(Boolean bodyMatches) {
        this.bodyMatches = bodyMatches;
    }

    public Boolean getHeadersMatch() {
        return headersMatch;
    }

    public void setHeadersMatch(Boolean headersMatch) {
        this.headersMatch = headersMatch;
    }

    public Boolean getStatusMatches() {
        return statusMatches;
    }

    public void setStatusMatches(Boolean statusMatches) {
        this.statusMatches = statusMatches;
    }

    public Boolean getMatches() {
        return matches;
    }

    public void setMatches(Boolean matches) {
        this.matches = matches;
    }
}

