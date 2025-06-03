package com.codebridge.session.model.enums;

import java.io.Serializable;

public enum ServerAuthProvider implements Serializable { // implements Serializable if used in serialized DTOs directly
    SSH_KEY,
    PASSWORD
}
