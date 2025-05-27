package com.codebridge.webhook.model;

/**
 * Enum representing the type of webhook.
 */
public enum WebhookType {
    /**
     * Webhook for GitHub events.
     */
    GITHUB,
    
    /**
     * Webhook for GitLab events.
     */
    GITLAB,
    
    /**
     * Webhook for Bitbucket events.
     */
    BITBUCKET,
    
    /**
     * Webhook for Docker Hub events.
     */
    DOCKER_HUB,
    
    /**
     * Webhook for Jenkins events.
     */
    JENKINS,
    
    /**
     * Webhook for Jira events.
     */
    JIRA,
    
    /**
     * Webhook for Slack events.
     */
    SLACK,
    
    /**
     * Webhook for custom events.
     */
    CUSTOM
}

