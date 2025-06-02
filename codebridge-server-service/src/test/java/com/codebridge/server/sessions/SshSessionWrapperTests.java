package com.codebridge.server.sessions;

import com.jcraft.jsch.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SshSessionWrapperTests {

    @Mock
    private Session jschSessionMock;

    private SessionKey sessionKey;
    private SshSessionWrapper sessionWrapper;

    @BeforeEach
    void setUp() {
        sessionKey = new SessionKey(UUID.randomUUID(), UUID.randomUUID(), "SSH");
        // sessionWrapper is created in each test where it's needed to control jschSessionMock behavior specifically
    }

    @Test
    void testConstructorAndGetters() {
        when(jschSessionMock.isConnected()).thenReturn(true); // Assume connected for basic construction
        sessionWrapper = new SshSessionWrapper(sessionKey, jschSessionMock);

        assertEquals(sessionKey, sessionWrapper.getSessionKey());
        assertEquals(jschSessionMock, sessionWrapper.getJschSession());
        assertTrue(sessionWrapper.getCreatedAt() > 0);
        assertEquals(sessionWrapper.getCreatedAt(), sessionWrapper.getLastAccessedTime());
        assertTrue(sessionWrapper.isConnected());
    }

    @Test
    void constructor_whenSessionKeyIsNull_shouldThrowIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new SshSessionWrapper(null, jschSessionMock);
        });
        assertEquals("SessionKey cannot be null.", exception.getMessage());
    }

    @Test
    void constructor_whenJschSessionIsNull_shouldThrowIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new SshSessionWrapper(sessionKey, null);
        });
        assertEquals("JSch Session cannot be null.", exception.getMessage());
    }


    @Test
    void testUpdateLastAccessedTime() throws InterruptedException {
        sessionWrapper = new SshSessionWrapper(sessionKey, jschSessionMock);
        long initialAccessTime = sessionWrapper.getLastAccessedTime();
        Thread.sleep(10); // Ensure time progresses
        sessionWrapper.updateLastAccessedTime();
        assertTrue(sessionWrapper.getLastAccessedTime() > initialAccessTime);
    }

    @Test
    void testIsConnected_whenJschSessionIsConnected_returnsTrue() {
        when(jschSessionMock.isConnected()).thenReturn(true);
        sessionWrapper = new SshSessionWrapper(sessionKey, jschSessionMock);
        assertTrue(sessionWrapper.isConnected());
    }

    @Test
    void testIsConnected_whenJschSessionIsNotConnected_returnsFalse() {
        when(jschSessionMock.isConnected()).thenReturn(false);
        sessionWrapper = new SshSessionWrapper(sessionKey, jschSessionMock);
        assertFalse(sessionWrapper.isConnected());
    }

    @Test
    void testDisconnect_whenJschSessionIsConnected_callsDisconnect() {
        // Setup logger to capture output
        Logger logger = (Logger) LoggerFactory.getLogger(SshSessionWrapper.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        when(jschSessionMock.isConnected()).thenReturn(true);
        sessionWrapper = new SshSessionWrapper(sessionKey, jschSessionMock);

        sessionWrapper.disconnect();

        verify(jschSessionMock, times(1)).disconnect();
        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("Disconnecting SSH session for key: " + sessionKey)));

        // Detach appender
        logger.detachAppender(listAppender);
    }

    @Test
    void testDisconnect_whenJschSessionIsNotConnected_doesNotCallDisconnect() {
        when(jschSessionMock.isConnected()).thenReturn(false);
        sessionWrapper = new SshSessionWrapper(sessionKey, jschSessionMock);

        sessionWrapper.disconnect();

        verify(jschSessionMock, never()).disconnect();
    }

    @Test
    void testDisconnect_whenJschSessionDisconnectThrowsException_logsError() {
        Logger logger = (Logger) LoggerFactory.getLogger(SshSessionWrapper.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.ERROR);


        when(jschSessionMock.isConnected()).thenReturn(true);
        doThrow(new RuntimeException("Disconnection error")).when(jschSessionMock).disconnect();
        sessionWrapper = new SshSessionWrapper(sessionKey, jschSessionMock);

        sessionWrapper.disconnect();

        verify(jschSessionMock, times(1)).disconnect();
        assertTrue(listAppender.list.stream()
            .anyMatch(event -> event.getLevel() == Level.ERROR &&
                               event.getFormattedMessage().contains("Error while disconnecting SSH session for key: " + sessionKey) &&
                               event.getThrowableProxy().getMessage().equals("Disconnection error")));

        logger.detachAppender(listAppender);
    }
}
