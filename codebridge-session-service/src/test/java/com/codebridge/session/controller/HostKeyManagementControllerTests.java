package com.codebridge.session.controller;

import com.codebridge.session.dto.HostKeyDTO;
import com.codebridge.session.dto.HostKeyVerificationPolicyDTO;
import com.codebridge.session.model.KnownSshHostKey;
import com.codebridge.session.repository.KnownSshHostKeyRepository;
import com.codebridge.session.service.CustomJschHostKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HostKeyManagementController.class)
class HostKeyManagementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnownSshHostKeyRepository hostKeyRepository;

    @MockBean
    private CustomJschHostKeyRepository customJschHostKeyRepository;

    private KnownSshHostKey testHostKey;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testHostKey = new KnownSshHostKey();
        testHostKey.setId(testId);
        testHostKey.setHostname("test.example.com");
        testHostKey.setPort(22);
        testHostKey.setKeyType("ssh-rsa");
        testHostKey.setHostKeyBase64("AAAAB3NzaC1yc2EAAAADAQABAAABAQC0eHn5WYYbYZk8kTUdllR+");
        testHostKey.setFingerprintSha256("SHA256:1234567890abcdef");
        testHostKey.setFirstSeen(LocalDateTime.now().minusDays(1));
        testHostKey.setLastVerified(LocalDateTime.now());

        when(customJschHostKeyRepository.getVerificationPolicy())
                .thenReturn(CustomJschHostKeyRepository.HostKeyVerificationPolicy.AUTO_ACCEPT);
    }

    @Test
    void getAllHostKeys_ShouldReturnAllKeys() throws Exception {
        List<KnownSshHostKey> hostKeys = Arrays.asList(testHostKey);
        when(hostKeyRepository.findAll()).thenReturn(hostKeys);

        mockMvc.perform(get("/api/host-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostKeys[0].id").value(testId.toString()))
                .andExpect(jsonPath("$.hostKeys[0].hostname").value("test.example.com"))
                .andExpect(jsonPath("$.hostKeys[0].keyType").value("ssh-rsa"))
                .andExpect(jsonPath("$.currentPolicy").value("AUTO_ACCEPT"));

        verify(hostKeyRepository).findAll();
    }

    @Test
    void getHostKeysByHost_ShouldReturnKeysForHost() throws Exception {
        List<KnownSshHostKey> hostKeys = Arrays.asList(testHostKey);
        when(hostKeyRepository.findByHostnameAndPort("test.example.com", 22)).thenReturn(hostKeys);

        mockMvc.perform(get("/api/host-keys/host")
                        .param("hostname", "test.example.com")
                        .param("port", "22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostKeys[0].id").value(testId.toString()))
                .andExpect(jsonPath("$.hostKeys[0].hostname").value("test.example.com"))
                .andExpect(jsonPath("$.currentPolicy").value("AUTO_ACCEPT"));

        verify(hostKeyRepository).findByHostnameAndPort("test.example.com", 22);
    }

    @Test
    void getHostKeyById_ShouldReturnKey() throws Exception {
        when(hostKeyRepository.findById(testId)).thenReturn(Optional.of(testHostKey));

        mockMvc.perform(get("/api/host-keys/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testId.toString()))
                .andExpect(jsonPath("$.hostname").value("test.example.com"));

        verify(hostKeyRepository).findById(testId);
    }

    @Test
    void getHostKeyById_NotFound_ShouldReturn404() throws Exception {
        when(hostKeyRepository.findById(testId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/host-keys/{id}", testId))
                .andExpect(status().isNotFound());

        verify(hostKeyRepository).findById(testId);
    }

    @Test
    void deleteHostKey_ShouldDeleteKey() throws Exception {
        when(hostKeyRepository.findById(testId)).thenReturn(Optional.of(testHostKey));

        mockMvc.perform(delete("/api/host-keys/{id}", testId))
                .andExpect(status().isNoContent());

        verify(hostKeyRepository).findById(testId);
        verify(hostKeyRepository).delete(testHostKey);
    }

    @Test
    void deleteHostKeysByHost_ShouldDeleteAllKeysForHost() throws Exception {
        List<KnownSshHostKey> hostKeys = Arrays.asList(testHostKey);
        when(hostKeyRepository.findByHostnameAndPort("test.example.com", 22)).thenReturn(hostKeys);

        mockMvc.perform(delete("/api/host-keys/host")
                        .param("hostname", "test.example.com")
                        .param("port", "22"))
                .andExpect(status().isNoContent());

        verify(hostKeyRepository).findByHostnameAndPort("test.example.com", 22);
        verify(hostKeyRepository).deleteAll(hostKeys);
    }

    @Test
    void addHostKey_NewKey_ShouldCreateKey() throws Exception {
        HostKeyDTO hostKeyDTO = new HostKeyDTO(
                null, "new.example.com", 22, "ssh-rsa",
                "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0eHn5WYYbYZk8kTUdllR+",
                "SHA256:1234567890abcdef", null, null);

        KnownSshHostKey newHostKey = new KnownSshHostKey();
        newHostKey.setId(UUID.randomUUID());
        newHostKey.setHostname("new.example.com");
        newHostKey.setPort(22);
        newHostKey.setKeyType("ssh-rsa");
        newHostKey.setHostKeyBase64("AAAAB3NzaC1yc2EAAAADAQABAAABAQC0eHn5WYYbYZk8kTUdllR+");
        newHostKey.setFingerprintSha256("SHA256:1234567890abcdef");
        newHostKey.setFirstSeen(LocalDateTime.now());
        newHostKey.setLastVerified(LocalDateTime.now());

        when(hostKeyRepository.findByHostnameAndPortAndKeyType("new.example.com", 22, "ssh-rsa"))
                .thenReturn(Optional.empty());
        when(hostKeyRepository.save(any(KnownSshHostKey.class))).thenReturn(newHostKey);

        mockMvc.perform(post("/api/host-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostKeyDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hostname").value("new.example.com"));

        verify(hostKeyRepository).findByHostnameAndPortAndKeyType("new.example.com", 22, "ssh-rsa");
        verify(hostKeyRepository).save(any(KnownSshHostKey.class));
    }

    @Test
    void addHostKey_ExistingKey_ShouldUpdateKey() throws Exception {
        HostKeyDTO hostKeyDTO = new HostKeyDTO(
                testId, "test.example.com", 22, "ssh-rsa",
                "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0eHn5WYYbYZk8kTUdllR+UPDATED",
                "SHA256:1234567890abcdef", null, null);

        KnownSshHostKey updatedHostKey = new KnownSshHostKey();
        updatedHostKey.setId(testId);
        updatedHostKey.setHostname("test.example.com");
        updatedHostKey.setPort(22);
        updatedHostKey.setKeyType("ssh-rsa");
        updatedHostKey.setHostKeyBase64("AAAAB3NzaC1yc2EAAAADAQABAAABAQC0eHn5WYYbYZk8kTUdllR+UPDATED");
        updatedHostKey.setFingerprintSha256("SHA256:1234567890abcdef");
        updatedHostKey.setFirstSeen(LocalDateTime.now().minusDays(1));
        updatedHostKey.setLastVerified(LocalDateTime.now());

        when(hostKeyRepository.findByHostnameAndPortAndKeyType("test.example.com", 22, "ssh-rsa"))
                .thenReturn(Optional.of(testHostKey));
        when(hostKeyRepository.save(any(KnownSshHostKey.class))).thenReturn(updatedHostKey);

        mockMvc.perform(post("/api/host-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostKeyDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostKeyBase64").value("AAAAB3NzaC1yc2EAAAADAQABAAABAQC0eHn5WYYbYZk8kTUdllR+UPDATED"));

        verify(hostKeyRepository).findByHostnameAndPortAndKeyType("test.example.com", 22, "ssh-rsa");
        verify(hostKeyRepository).save(any(KnownSshHostKey.class));
    }

    @Test
    void getVerificationPolicy_ShouldReturnPolicy() throws Exception {
        mockMvc.perform(get("/api/host-keys/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy").value("AUTO_ACCEPT"));

        verify(customJschHostKeyRepository).getVerificationPolicy();
    }

    @Test
    void setVerificationPolicy_ShouldUpdatePolicy() throws Exception {
        HostKeyVerificationPolicyDTO policyDTO = new HostKeyVerificationPolicyDTO("STRICT");

        mockMvc.perform(put("/api/host-keys/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy").value("STRICT"));

        verify(customJschHostKeyRepository).setVerificationPolicy(
                CustomJschHostKeyRepository.HostKeyVerificationPolicy.STRICT);
    }

    @Test
    void setVerificationPolicy_InvalidPolicy_ShouldReturn400() throws Exception {
        HostKeyVerificationPolicyDTO policyDTO = new HostKeyVerificationPolicyDTO("INVALID_POLICY");

        doThrow(new IllegalArgumentException("Invalid policy"))
                .when(customJschHostKeyRepository).setVerificationPolicy(any());

        mockMvc.perform(put("/api/host-keys/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyDTO)))
                .andExpect(status().isBadRequest());
    }
}

