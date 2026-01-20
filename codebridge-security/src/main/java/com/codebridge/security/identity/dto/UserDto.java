package com.codebridge.security.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profileImageUrl;
    private boolean enabled;
    private Set<String> roles;
    private Set<OrganizationDto> organizations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // Manual builder method
    public static UserDtoBuilder builder() {
        return new UserDtoBuilder();
    }

    // Manual builder class
    public static class UserDtoBuilder {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String profileImageUrl;
        private boolean enabled;
        private Set<String> roles;
        private Set<OrganizationDto> organizations;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastLoginAt;

        public UserDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserDtoBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserDtoBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserDtoBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserDtoBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserDtoBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UserDtoBuilder profileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            return this;
        }

        public UserDtoBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserDtoBuilder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        public UserDtoBuilder organizations(Set<OrganizationDto> organizations) {
            this.organizations = organizations;
            return this;
        }

        public UserDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserDtoBuilder lastLoginAt(LocalDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
            return this;
        }

        public UserDto build() {
            UserDto dto = new UserDto();
            dto.id = this.id;
            dto.username = this.username;
            dto.email = this.email;
            dto.firstName = this.firstName;
            dto.lastName = this.lastName;
            dto.phoneNumber = this.phoneNumber;
            dto.profileImageUrl = this.profileImageUrl;
            dto.enabled = this.enabled;
            dto.roles = this.roles;
            dto.organizations = this.organizations;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            dto.lastLoginAt = this.lastLoginAt;
            return dto;
        }
    }
}
