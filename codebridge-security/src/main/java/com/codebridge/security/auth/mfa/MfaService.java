package com.codebridge.security.auth.mfa;

import com.codebridge.security.auth.model.User;
import com.codebridge.security.auth.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Service for multi-factor authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private final UserRepository userRepository;
    
    @Value("${security.mfa.issuer}")
    private String issuer;
    
    @Value("${security.mfa.time-step-seconds}")
    private int timeStepSeconds;
    
    @Value("${security.mfa.code-digits}")
    private int codeDigits;
    
    @Value("${security.mfa.allowed-time-skew}")
    private int allowedTimeSkew;

    /**
     * Generates a new MFA secret for a user.
     *
     * @param user The user
     * @return The MFA secret
     */
    @Transactional
    public String generateMfaSecret(User user) {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();
        
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        userRepository.save(user);
        
        return secret;
    }

    /**
     * Generates a QR code for MFA setup.
     *
     * @param user The user
     * @param secret The MFA secret
     * @return The QR code as a data URI
     * @throws QrGenerationException If the QR code generation fails
     */
    public String generateQrCodeImageUri(User user, String secret) throws QrGenerationException {
        QrData data = new QrData.Builder()
                .label(user.getUsername())
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(codeDigits)
                .period(timeStepSeconds)
                .build();

        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        byte[] imageData = qrGenerator.generate(data);
        
        return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
    }

    /**
     * Verifies an MFA code.
     *
     * @param user The user
     * @param code The MFA code
     * @return True if the code is valid, false otherwise
     */
    public boolean verifyCode(User user, String code) {
        if (user == null || !user.isMfaEnabled() || user.getMfaSecret() == null) {
            return false;
        }

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        
        // Set allowed time skew
        if (verifier instanceof DefaultCodeVerifier) {
            ((DefaultCodeVerifier) verifier).setAllowedTimePeriodDiscrepancy(allowedTimeSkew);
        }
        
        return verifier.isValidCode(user.getMfaSecret(), code);
    }

    /**
     * Enables MFA for a user.
     *
     * @param user The user
     * @param code The MFA code
     * @return True if MFA was enabled, false otherwise
     */
    @Transactional
    public boolean enableMfa(User user, String code) {
        if (verifyCode(user, code)) {
            user.setMfaEnabled(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Disables MFA for a user.
     *
     * @param user The user
     */
    @Transactional
    public void disableMfa(User user) {
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }

    /**
     * Generates backup codes for a user.
     *
     * @param user The user
     * @return The backup codes
     */
    public String[] generateBackupCodes() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String[] backupCodes = new String[10];
        
        for (int i = 0; i < 10; i++) {
            // Generate a random string and take first 8 characters
            String randomCode = secretGenerator.generate();
            backupCodes[i] = Base64.getEncoder()
                    .encodeToString(randomCode.getBytes())
                    .substring(0, 8)
                    .toUpperCase();
        }
        
        return backupCodes;
    }
}

