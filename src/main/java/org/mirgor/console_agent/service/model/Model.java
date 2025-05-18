package org.mirgor.console_agent.service.model;

import lombok.Getter;


@Getter
public enum Model {
    GPT_4_1("gpt-4.1", SystemPromptType.CODE, 1_047_576),
    GPT_O3_MINI("o3-mini", SystemPromptType.REASONING, 200_000),
    CLAUDE_SONNET_3_7("claude-3-7-sonnet-latest", SystemPromptType.CODE, 200_000);

    private final String label;
    private final SystemPromptType promptType;
    private final long contextWindowSize;

    Model(String label, SystemPromptType promptType, long contextWindowSize) {
        this.label = label;
        this.promptType = promptType;
        this.contextWindowSize = contextWindowSize;
    }

    public String getSystemPromptFileName() {
        return promptType.getFileName();
    }


    @Getter
    public enum SystemPromptType {
        CODE("/system_prompt_code.txt"),
        REASONING("/system_prompt_reasoning.txt");

        private final String fileName;

        SystemPromptType(String fileName) {
            this.fileName = fileName;
        }
    }
}