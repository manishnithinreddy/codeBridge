package com.codebridge.usermanagement.settings.controller;

import com.codebridge.usermanagement.common.model.SettingType;
import com.codebridge.usermanagement.settings.model.ApplicationSetting;
import com.codebridge.usermanagement.settings.service.ApplicationSettingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for application setting operations.
 */
@RestController
@RequestMapping("/settings")
public class ApplicationSettingController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationSettingController.class);

    private final ApplicationSettingService applicationSettingService;

    @Autowired
    public ApplicationSettingController(ApplicationSettingService applicationSettingService) {
        this.applicationSettingService = applicationSettingService;
    }

    /**
     * Get all application settings.
     *
     * @return list of application settings
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationSetting>> getAllSettings() {
        logger.info("Getting all application settings");
        List<ApplicationSetting> settings = applicationSettingService.getAllSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * Get a setting by ID.
     *
     * @param id the setting ID
     * @return the setting
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationSetting> getSettingById(@PathVariable UUID id) {
        logger.info("Getting application setting by ID: {}", id);
        ApplicationSetting setting = applicationSettingService.getSettingById(id);
        return ResponseEntity.ok(setting);
    }

    /**
     * Get a setting by key.
     *
     * @param key the setting key
     * @return the setting
     */
    @GetMapping("/key/{key}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApplicationSetting> getSettingByKey(@PathVariable String key) {
        logger.info("Getting application setting by key: {}", key);
        ApplicationSetting setting = applicationSettingService.getSettingByKey(key);
        return ResponseEntity.ok(setting);
    }

    /**
     * Get all settings in a specific category.
     *
     * @param category the category
     * @return list of settings
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationSetting>> getSettingsByCategory(@PathVariable String category) {
        logger.info("Getting application settings by category: {}", category);
        List<ApplicationSetting> settings = applicationSettingService.getSettingsByCategory(category);
        return ResponseEntity.ok(settings);
    }

    /**
     * Get all global settings.
     *
     * @return list of global settings
     */
    @GetMapping("/global")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationSetting>> getGlobalSettings() {
        logger.info("Getting global application settings");
        List<ApplicationSetting> settings = applicationSettingService.getGlobalSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * Get all settings for a specific team.
     *
     * @param teamId the team ID
     * @return list of team settings
     */
    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationSetting>> getTeamSettings(@PathVariable UUID teamId) {
        logger.info("Getting application settings for team ID: {}", teamId);
        List<ApplicationSetting> settings = applicationSettingService.getTeamSettings(teamId);
        return ResponseEntity.ok(settings);
    }

    /**
     * Get all settings for a specific team in a specific category.
     *
     * @param teamId the team ID
     * @param category the category
     * @return list of team settings
     */
    @GetMapping("/team/{teamId}/category/{category}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationSetting>> getTeamSettingsByCategory(
            @PathVariable UUID teamId, @PathVariable String category) {
        logger.info("Getting application settings for team ID: {} in category: {}", teamId, category);
        List<ApplicationSetting> settings = applicationSettingService.getTeamSettingsByCategory(teamId, category);
        return ResponseEntity.ok(settings);
    }

    /**
     * Get a specific setting for a team.
     *
     * @param teamId the team ID
     * @param key the setting key
     * @return the setting
     */
    @GetMapping("/team/{teamId}/key/{key}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApplicationSetting> getTeamSetting(@PathVariable UUID teamId, @PathVariable String key) {
        logger.info("Getting application setting for team ID: {}, key: {}", teamId, key);
        ApplicationSetting setting = applicationSettingService.getTeamSetting(teamId, key);
        return ResponseEntity.ok(setting);
    }

    /**
     * Create a new application setting.
     *
     * @param applicationSetting the setting to create
     * @return the created setting
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationSetting> createSetting(@Valid @RequestBody ApplicationSetting applicationSetting) {
        logger.info("Creating application setting with key: {}", applicationSetting.getKey());
        ApplicationSetting createdSetting = applicationSettingService.createSetting(applicationSetting);
        return new ResponseEntity<>(createdSetting, HttpStatus.CREATED);
    }

    /**
     * Update an application setting.
     *
     * @param id the setting ID
     * @param applicationSettingDetails the setting details
     * @return the updated setting
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationSetting> updateSetting(
            @PathVariable UUID id, @Valid @RequestBody ApplicationSetting applicationSettingDetails) {
        logger.info("Updating application setting with ID: {}", id);
        ApplicationSetting updatedSetting = applicationSettingService.updateSetting(id, applicationSettingDetails);
        return ResponseEntity.ok(updatedSetting);
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
    @PutMapping("/global/key/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationSetting> setGlobalSetting(
            @PathVariable String key,
            @RequestParam String value,
            @RequestParam SettingType type,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean isSecret) {
        logger.info("Setting global application setting with key: {}", key);
        
        // Default to false if not provided
        boolean secret = isSecret != null ? isSecret : false;
        
        ApplicationSetting setting = applicationSettingService.setGlobalSetting(
                key, value, type, category, description, secret);
        return ResponseEntity.ok(setting);
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
    @PutMapping("/team/{teamId}/key/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationSetting> setTeamSetting(
            @PathVariable UUID teamId,
            @PathVariable String key,
            @RequestParam String value,
            @RequestParam SettingType type,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean isSecret) {
        logger.info("Setting team application setting with key: {} for team ID: {}", key, teamId);
        
        // Default to false if not provided
        boolean secret = isSecret != null ? isSecret : false;
        
        ApplicationSetting setting = applicationSettingService.setTeamSetting(
                teamId, key, value, type, category, description, secret);
        return ResponseEntity.ok(setting);
    }

    /**
     * Delete an application setting.
     *
     * @param id the setting ID
     * @return no content if the setting was deleted
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSetting(@PathVariable UUID id) {
        logger.info("Deleting application setting with ID: {}", id);
        boolean deleted = applicationSettingService.deleteSetting(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get application settings as a map.
     *
     * @return map of setting keys to values
     */
    @GetMapping("/map")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getSettingsAsMap() {
        logger.info("Getting application settings as map");
        Map<String, String> settings = applicationSettingService.getSettingsAsMap();
        return ResponseEntity.ok(settings);
    }

    /**
     * Get global application settings as a map.
     *
     * @return map of setting keys to values
     */
    @GetMapping("/global/map")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getGlobalSettingsAsMap() {
        logger.info("Getting global application settings as map");
        Map<String, String> settings = applicationSettingService.getGlobalSettingsAsMap();
        return ResponseEntity.ok(settings);
    }

    /**
     * Get team application settings as a map.
     *
     * @param teamId the team ID
     * @return map of setting keys to values
     */
    @GetMapping("/team/{teamId}/map")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getTeamSettingsAsMap(@PathVariable UUID teamId) {
        logger.info("Getting application settings as map for team ID: {}", teamId);
        Map<String, String> settings = applicationSettingService.getTeamSettingsAsMap(teamId);
        return ResponseEntity.ok(settings);
    }

    /**
     * Set multiple global application settings at once.
     *
     * @param settings map of setting keys to values
     * @param type the setting type
     * @param category the setting category
     * @return list of updated or created settings
     */
    @PutMapping("/global/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationSetting>> setGlobalSettings(
            @RequestBody Map<String, String> settings,
            @RequestParam SettingType type,
            @RequestParam String category) {
        logger.info("Setting multiple global application settings");
        List<ApplicationSetting> updatedSettings = applicationSettingService.setGlobalSettings(
                settings, type, category);
        return ResponseEntity.ok(updatedSettings);
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
    @PutMapping("/team/{teamId}/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationSetting>> setTeamSettings(
            @PathVariable UUID teamId,
            @RequestBody Map<String, String> settings,
            @RequestParam SettingType type,
            @RequestParam String category) {
        logger.info("Setting multiple application settings for team ID: {}", teamId);
        List<ApplicationSetting> updatedSettings = applicationSettingService.setTeamSettings(
                teamId, settings, type, category);
        return ResponseEntity.ok(updatedSettings);
    }
}
