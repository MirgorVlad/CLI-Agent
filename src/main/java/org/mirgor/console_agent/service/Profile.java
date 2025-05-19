package org.mirgor.console_agent.service;

import lombok.Getter;
import org.mirgor.console_agent.service.model.Model;

@Getter
public enum Profile {
    CODE(Model.CLAUDE_SONNET_3_7, "/system_prompt_code.txt"),
    REASONING(Model.GPT_O3_MINI, "/system_prompt_reasoning.txt"),
    SIMPLE(Model.GPT_4_1, null);

    private final Model model;
    private final String systemPromptFile;

    Profile(Model model, String systemPromptFile) {
        this.model = model;
        this.systemPromptFile = systemPromptFile;
    }
}
