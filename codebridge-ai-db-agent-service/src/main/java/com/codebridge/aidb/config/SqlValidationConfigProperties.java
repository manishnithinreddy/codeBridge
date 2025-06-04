package com.codebridge.aidb.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set; // Using Set to automatically handle duplicates from property string
import java.util.HashSet;
import java.util.Arrays;
import java.util.stream.Collectors;


@Configuration
@ConfigurationProperties(prefix = "codebridge.ai.sql-validation")
@Validated
public class SqlValidationConfigProperties {

    @NotEmpty(message = "Denied keywords list cannot be empty")
    private String deniedKeywords = "DROP,DELETE,INSERT,UPDATE,CREATE,ALTER,TRUNCATE,GRANT,REVOKE,COMMIT,ROLLBACK,SAVEPOINT,SHUTDOWN,EXEC,EXECUTE";

    private boolean allowOnlySelectForReadonly = true;

    public Set<String> getDeniedKeywordsSet() {
        // Convert comma-separated string to a Set of uppercase keywords
        if (deniedKeywords == null || deniedKeywords.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(deniedKeywords.split(","))
                     .map(String::trim)
                     .map(String::toUpperCase)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toSet());
    }

    public String getDeniedKeywords() {
        return deniedKeywords;
    }

    public void setDeniedKeywords(String deniedKeywords) {
        this.deniedKeywords = deniedKeywords;
    }

    public boolean isAllowOnlySelectForReadonly() {
        return allowOnlySelectForReadonly;
    }

    public void setAllowOnlySelectForReadonly(boolean allowOnlySelectForReadonly) {
        this.allowOnlySelectForReadonly = allowOnlySelectForReadonly;
    }
}
