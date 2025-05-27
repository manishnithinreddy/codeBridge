package com.codebridge.usermanagement.settings.repository;

import com.codebridge.usermanagement.settings.model.ApplicationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ApplicationSetting entity operations.
 */
@Repository
public interface ApplicationSettingRepository extends JpaRepository<ApplicationSetting, UUID> {

    /**
     * Find a setting by key.
     *
     * @param key the setting key
     * @return the setting if found
     */
    Optional<ApplicationSetting> findByKey(String key);

    /**
     * Find all settings in a specific category.
     *
     * @param category the category
     * @return list of settings
     */
    List<ApplicationSetting> findByCategory(String category);

    /**
     * Find all global settings.
     *
     * @param isGlobal the global status
     * @return list of global settings
     */
    List<ApplicationSetting> findByIsGlobal(boolean isGlobal);

    /**
     * Find all settings for a specific team.
     *
     * @param teamId the team ID
     * @return list of team settings
     */
    List<ApplicationSetting> findByTeamId(UUID teamId);

    /**
     * Find all settings for a specific team in a specific category.
     *
     * @param teamId the team ID
     * @param category the category
     * @return list of team settings
     */
    List<ApplicationSetting> findByTeamIdAndCategory(UUID teamId, String category);

    /**
     * Find a specific setting for a team.
     *
     * @param teamId the team ID
     * @param key the setting key
     * @return the setting if found
     */
    Optional<ApplicationSetting> findByTeamIdAndKey(UUID teamId, String key);

    /**
     * Find all secret settings.
     *
     * @param isSecret the secret status
     * @return list of secret settings
     */
    List<ApplicationSetting> findByIsSecret(boolean isSecret);

    /**
     * Find all encrypted settings.
     *
     * @param isEncrypted the encrypted status
     * @return list of encrypted settings
     */
    List<ApplicationSetting> findByIsEncrypted(boolean isEncrypted);
}
