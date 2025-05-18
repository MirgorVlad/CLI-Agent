package org.mirgor.console_agent.service;

import org.mirgor.console_agent.service.model.Model;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {

    private Map<String, LlmDetails> llmConfigs;

    public Map<String, LlmDetails> getLlmConfigs() {
        return llmConfigs;
    }

    public void setLlmConfigs(Map<String, LlmDetails> llmConfigs) {
        this.llmConfigs = llmConfigs;
    }

    public LlmDetails get(Model service) {
        return llmConfigs.get(service.name().toLowerCase());
    }

    public record LlmDetails(String key, String endpoint) {

    }

}
