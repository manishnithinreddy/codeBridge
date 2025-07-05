package com.codebridge.security.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {
    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    private String websiteUrl;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Manual builder method
    public static OrganizationDtoBuilder builder() {
        return new OrganizationDtoBuilder();
    }

    // Manual builder class
    public static class OrganizationDtoBuilder {
        private Long id;
        private String name;
        private String description;
        private String logoUrl;
        private String websiteUrl;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OrganizationDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrganizationDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public OrganizationDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public OrganizationDtoBuilder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public OrganizationDtoBuilder websiteUrl(String websiteUrl) {
            this.websiteUrl = websiteUrl;
            return this;
        }

        public OrganizationDtoBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public OrganizationDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrganizationDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public OrganizationDto build() {
            OrganizationDto dto = new OrganizationDto();
            dto.id = this.id;
            dto.name = this.name;
            dto.description = this.description;
            dto.logoUrl = this.logoUrl;
            dto.websiteUrl = this.websiteUrl;
            dto.active = this.active;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            return dto;
        }
    }
}
