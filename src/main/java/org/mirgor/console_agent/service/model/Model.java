package org.mirgor.console_agent.service.model;

import lombok.Getter;

@Getter
public enum Model {
    GPT_4_1("gpt-4.1", 1_047_576),
    GPT_O3_MINI("o3-mini", 200_000),
    CLAUDE_SONNET_3_7("anthropic/claude-3.7-sonnet", 200_000);

    private final String label;
    private final long contextWindowSize;

    Model(String label, long contextWindowSize) {
        this.label = label;
        this.contextWindowSize = contextWindowSize;
    }
}
