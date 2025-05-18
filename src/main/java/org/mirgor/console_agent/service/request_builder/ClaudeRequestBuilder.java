package org.mirgor.console_agent.service.request_builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mirgor.console_agent.service.model.ChatMessage;
import org.mirgor.console_agent.service.model.Model;
import org.mirgor.console_agent.service.model.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class ClaudeRequestBuilder extends ModelRequestBuilder{

    @Override
    public List<Model> getModels() {
        return List.of(Model.CLAUDE_SONNET_3_7);
    }

    @Override
    public ChatMessage getSystemPromptMessage(String prompt) {
        return new ChatMessage(Role.USER, prompt);
    }

    @Override
    public HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
        httpHeaders.add("x-api-key", apiKey);
        httpHeaders.add("anthropic-version", "2023-06-01");
        return httpHeaders;
    }

    @Override
    public String buildRequestBody(Model model, List<ChatMessage> context) throws JsonProcessingException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode messages = objectMapper.valueToTree(context);
        requestBody.put("model", model.getLabel());
        requestBody.put("max_tokens", 10_000);
        requestBody.set("messages", messages);

        return objectMapper.writeValueAsString(requestBody);
    }

    @Override
    public ChatMessage parseResponse(ResponseEntity<String> response) {
        if (response == null || response.getBody() == null) {
            return new ChatMessage(Role.ASSISTANT, "");
        }

        try {
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            JsonNode output = responseNode.get("content");
            String responseMessage = StreamSupport.stream(output.spliterator(), false)
                    .map(contentPart -> contentPart.path("text").asText())
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining());
            return new ChatMessage(Role.ASSISTANT, responseMessage);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse model response", e);
        }
    }

}
