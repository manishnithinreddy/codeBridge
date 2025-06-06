package com.codebridge.apitest.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * JPA converter that automatically encrypts/decrypts sensitive string values
 * when storing/retrieving from the database.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final TextEncryptor encryptor;

    public EncryptedStringConverter(
            @Value("${codebridge.encryption.password:defaultEncryptionPassword}") String password,
            @Value("${codebridge.encryption.salt:5c0d3br1dg3}") String salt) {
        // Use Spring's Encryptors utility to create a secure text encryptor
        this.encryptor = Encryptors.text(password, salt);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // Encrypt the value before storing in the database
        return attribute != null ? encryptor.encrypt(attribute) : null;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // Decrypt the value when reading from the database
        return dbData != null ? encryptor.decrypt(dbData) : null;
    }
}

