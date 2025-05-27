package com.codebridge.usermanagement.settings.service;

import com.codebridge.usermanagement.common.exception.ResourceNotFoundException;
import com.codebridge.usermanagement.common.model.SettingType;
import com.codebridge.usermanagement.settings.model.ApplicationSetting;
import com.codebridge.usermanagement.settings.repository.ApplicationSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for application setting operations.
 */
@Service
public class ApplicationSettingService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationSettingService.class);

    private final ApplicationSettingRepository applicationSettingRepository;

    @Autowired
    public ApplicationSettingService(ApplicationSettingRepository applicationSettingRepository) {
        this.applicationSettingRepository = applicationSettingRepository;
    }

    /**
     * Get all application settings.
     *
     * @return list of application settings
     */
    public List<ApplicationSetting> getAllSettings() {
        return applicationSettingRepository.findAll();
    }

    /**
     * Get a setting by ID.
     *
     * @param id the setting ID
     * @return the setting
     * @throws ResourceNotFoundException if the setting is not found
     */
    public ApplicationSetting getSettingById(UUID id) {
        return applicationSettingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationSetting", "id", id));
    }

    /**
     * Get a setting by key.
     *
     * @param key the setting key
     * @return the setting
     * @throws ResourceNotFoundException if the setting is not found
     */
    public ApplicationSetting getSettingByKey(String key) {
        return applicationSettingRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationSetting", "key", key));
    }

    /**
     * Get all settings in a specific category.
     *
     * @param category the category
     * @return list of settings
     */
    public List<ApplicationSetting> getSettingsByCategory(String category) {
        return applicationSettingRepository.findByCategory(category);
    }

    /**
     * Get all global settings.
     *
     * @return list of global settings
     */
    public List<ApplicationSetting> getGlobalSettings() {
        return applicationSettingRepository.findByIsGlobal(true);
    }

    /**
     * Get all settings for a specific team.
     *
     * @param teamId the team ID
     * @return list of team settings
     */
    public List<ApplicationSetting> getTeamSettings(UUID teamId) {
        return applicationSettingRepository.findByTeamId(teamId);
    }

    /**
     * Get all settings for a specific team in a specific category.
     *
     * @param teamId the team ID
     * @param category the category
     * @return list of team settings
     */
    public List<ApplicationSetting> getTeamSettingsByCategory(UUID teamId, String category) {
        return applicationSettingRepository.findByTeamIdAndCategory(teamId, category);
    }

    /**
     * Get a specific setting for a team.
     *
     * @param teamId the team ID
     * @param key the setting key
     * @return the setting
     * @throws ResourceNotFoundException if the setting is not found
     */
    public ApplicationSetting getTeamSetting(UUID teamId, String key) {
        return applicationSettingRepository.findByTeamIdAndKey(teamId, key)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationSetting", "key", key));
    }

    /**
     * Create a new application setting.
     *
     * @param applicationSetting the setting to create
     * @return the created setting
     */
    @Transactional
    public ApplicationSetting createSetting(ApplicationSetting applicationSetting) {
        if (applicationSetting.getId() == null) {
            applicationSetting.setId(UUID.randomUUID());
        }
        
        logger.info("Creating application setting with key: {}", applicationSetting.getKey());
        return applicationSettingRepository.save(applicationSetting);
    }

    /**
     * Update an application setting.
     *
     * @param id the setting ID
     * @param applicationSettingDetails the setting details
     * @return the updated setting
     * @throws ResourceNotFoundException if the setting is not found
     */
    @Transactional
    public ApplicationSetting updateSetting(UUID id, ApplicationSetting applicationSettingDetails) {
        ApplicationSetting applicationSetting = getSettingById(id);
        
        applicationSetting.setValue(applicationSettingDetails.getValue());
        applicationSetting.setDescription(applicationSettingDetails.getDescription());
        applicationSetting.setType(applicationSettingDetails.getType());
        applicationSetting.setCategory(applicationSettingDetails.getCategory());
        applicationSetting.setGlobal(applicationSettingDetails.isGlobal());
        applicationSetting.setTeamId(applicationSettingDetails.getTeamId());
        applicationSetting.setSecret(applicationSettingDetails.isSecret());
        applicationSetting.setEncrypted(applicationSettingDetails.isEncrypted());
        
        logger.info("Updating application setting with key: {}", applicationSetting.getKey());
        return applicationSettingRepository.save(applicationSetting);
    }

    /**
     * Set a global application setting.
     *
     * @param key the setting key
     * @param value the setting value
     * @param type the setting type
     * @param category the setting category
     * @param description the setting description
     * @param isSecret whether the setting is secret
     * @return the updated or created setting
     */
    @Transactional
    public ApplicationSetting setGlobalSetting(String key, String value, SettingType type, 
                                             String category, String description, boolean isSecret) {
        Optional<ApplicationSetting> existingSetting = applicationSettingRepository.findByKey(key);
        
        if (existingSetting.isPresent()) {
            ApplicationSetting applicationSetting = existingSetting.get();
            applicationSetting.setValue(value);
            applicationSetting.setType(type);
            applicationSetting.setCategory(category);
            applicationSetting.setDescription(description);
            applicationSetting.setSecret(isSecret);
            applicationSetting.setGlobal(true);
            applicationSetting.setTeamId(null);
            
            logger.info("Updating global application setting with key: {}", key);
            return applicationSettingRepository.save(applicationSetting);
        } else {
            ApplicationSetting applicationSetting = new ApplicationSetting();
            applicationSetting.setId(UUID.randomUUID());
            applicationSetting.setKey(key);
            applicationSetting.setValue(value);
            applicationSetting.setType(type);
            applicationSetting.setCategory(category);
            applicationSetting.setDescription(description);
            applicationSetting.setSecret(isSecret);
            applicationSetting.setGlobal(true);
            applicationSetting.setTeamId(null);
            
            logger.info("Creating global application setting with key: {}", key);
            return applicationSettingRepository.save(applicationSetting);
        }
    }

    /**
     * Set a team application setting.
     *
     * @param teamId the team ID
     * @param key the setting key
     * @param value the setting value
     * @param type the setting type
     * @param category the setting category
     * @param description the setting description
     * @param isSecret whether the setting is secret
     * @return the updated or created setting
     */
    @Transactional
    public ApplicationSetting setTeamSetting(UUID teamId, String key, String value, SettingType type, 
                                           String category, String description, boolean isSecret) {
        Optional<ApplicationSetting> existingSetting = applicationSettingRepository.findByTeamIdAndKey(teamId, key);
        
        if (existingSetting.isPresent()) {
            ApplicationSetting applicationSetting = existingSetting.get();
            applicationSetting.setValue(value);
            applicationSetting.setType(type);
            applicationSetting.setCategory(category);
            applicationSetting.setDescription(description);
            applicationSetting.setSecret(isSecret);
            applicationSetting.setGlobal(false);
            
            logger.info("Updating team application setting with key: {} for team ID: {}", key, teamId);
            return applicationSettingRepository.save(applicationSetting);
        } else {
            ApplicationSetting applicationSetting = new ApplicationSetting();
            applicationSetting.setId(UUID.randomUUID());
            applicationSetting.setKey(key);
            applicationSetting.setValue(value);
            applicationSetting.setType(type);
            applicationSetting.setCategory(category);
            applicationSetting.setDescription(description);
            applicationSetting.setSecret(isSecret);
            applicationSetting.setGlobal(false);
            applicationSetting.setTeamId(teamId);
            
            logger.info("Creating team application setting with key: {} for team ID: {}", key, teamId);
            return applicationSettingRepository.save(applicationSetting);
        }
    }

    /**
     * Delete an application setting.
     *
     * @param id the setting ID
     * @return true if the setting was deleted, false otherwise
     */
    @Transactional
    public boolean deleteSetting(UUID id) {
        Optional<ApplicationSetting> applicationSetting = applicationSettingRepository.findById(id);
        if (applicationSetting.isPresent()) {
            applicationSettingRepository.delete(applicationSetting.get());
            logger.info("Deleted application setting with ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Get application settings as a map.
     *
     * @return map of setting keys to values
     */
    public Map<String, String> getSettingsAsMap() {
        List<ApplicationSetting> settings = applicationSettingRepository.findAll();
        return settings.stream()
                .collect(Collectors.toMap(ApplicationSetting::getKey, ApplicationSetting::getValue));
    }

    /**
     * Get global application settings as a map.
     *
     * @return map of setting keys to values
     */
    public Map<String, String> getGlobalSettingsAsMap() {
        List<ApplicationSetting> settings = applicationSettingRepository.findByIsGlobal(true);
        return settings.stream()
                .collect(Collectors.toMap(ApplicationSetting::getKey, ApplicationSetting::getValue));
    }

    /**
     * Get team application settings as a map.
     *
     * @param teamId the team ID
     * @return map of setting keys to values
     */
    public Map<String, String> getTeamSettingsAsMap(UUID teamId) {
        List<ApplicationSetting> settings = applicationSettingRepository.findByTeamId(teamId);
        return settings.stream()
                .collect(Collectors.toMap(ApplicationSetting::getKey, ApplicationSetting::getValue));
    }

    /**
     * Set multiple global application settings at once.
     *
     * @param settings map of setting keys to values
     * @param type the setting type
     * @param category the setting category
     * @return list of updated or created settings
     */
    @Transactional
    public List<ApplicationSetting> setGlobalSettings(Map<String, String> settings, SettingType type, String category) {
        return settings.entrySet().stream()
                .map(entry -> setGlobalSetting(entry.getKey(), entry.getValue(), type, category, null, false))
                .collect(Collectors.toList());
    }

    /**
     * Set multiple team application settings at once.
     *
     * @param teamId the team ID
     * @param settings map of setting keys to values
     * @param type the setting type
     * @param category the setting category
     * @return list of updated or created settings
     */
    @Transactional
    public List<ApplicationSetting> setTeamSettings(UUID teamId, Map<String, String> settings, SettingType type, String category) {
        return settings.entrySet().stream()
                .map(entry -> setTeamSetting(teamId, entry.getKey(), entry.getValue(), type, category, null, false))
                .collect(Collectors.toList());
    }
}
