package org.mirgor.console_agent.service.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    USER,
    DEVELOPER,
    ASSISTANT;

    @JsonValue
    public String toLowerCase() {
        return name().toLowerCase();
    }
}
