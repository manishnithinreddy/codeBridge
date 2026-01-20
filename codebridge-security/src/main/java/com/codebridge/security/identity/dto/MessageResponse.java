package com.codebridge.security.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;

    // Manual builder method
    public static MessageResponseBuilder builder() {
        return new MessageResponseBuilder();
    }

    // Manual builder class
    public static class MessageResponseBuilder {
        private String message;

        public MessageResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public MessageResponse build() {
            MessageResponse response = new MessageResponse();
            response.message = this.message;
            return response;
        }
    }

    // Manual getter method
    public String getMessage() {
        return message;
    }
}
