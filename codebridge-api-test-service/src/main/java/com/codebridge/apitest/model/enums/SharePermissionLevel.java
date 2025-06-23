package com.codebridge.apitest.model.enums;

public enum SharePermissionLevel {
    NO_ACCESS,    // No access to the project
    CAN_VIEW,     // Can view project, collections, tests (read-only)
    VIEW_ONLY,    // Alias for CAN_VIEW (for backward compatibility)
    CAN_EXECUTE,  // CAN_VIEW + can execute tests/collections
    CAN_EDIT      // CAN_VIEW + CAN_EXECUTE + can edit project/collections/tests, manage collections within project
}

