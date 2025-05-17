package org.mirgor.console_agent.service;

import lombok.Getter;

@Getter
public enum Model {
    GPT_4_1("gpt-4.1"),
    GPT_O3_MINI("o3-mini"),
    CLAUDE_SONNET_3_7("anthropic/claude-3.7-sonnet");

    private final String label;

    Model(String label) {
        this.label = label;
    }
}
