package com.codebridge.session.dto;

import java.io.Serializable;

public record CommandResponse(
    String output,
    int exitCode
) implements Serializable {
}

