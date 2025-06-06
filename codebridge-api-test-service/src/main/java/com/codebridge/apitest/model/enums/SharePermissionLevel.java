package com.codebridge.apitest.model.enums;

public enum SharePermissionLevel {
    VIEW_ONLY,    // Can view project, collections, tests (read-only)
    CAN_EXECUTE,  // VIEW_ONLY + can execute tests/collections
    CAN_EDIT      // VIEW_ONLY + CAN_EXECUTE + can edit project/collections/tests, manage collections within project
}
