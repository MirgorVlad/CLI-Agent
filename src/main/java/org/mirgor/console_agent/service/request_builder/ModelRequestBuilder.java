package org.mirgor.console_agent.service.request_builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mirgor.console_agent.service.model.Model;
import org.mirgor.console_agent.service.model.ChatMessage;
import org.mirgor.console_agent.service.model.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class ModelRequestBuilder {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    public abstract List<Model> getModels();

    public HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", apiKey));
        return httpHeaders;
    }

    public String buildRequestBody(Model model, List<ChatMessage> context) throws JsonProcessingException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode messages = objectMapper.valueToTree(context);
        requestBody.put("model", model.getLabel());
        requestBody.set("messages", messages);

        return objectMapper.writeValueAsString(requestBody);
    }

    public ChatMessage parseResponse(ResponseEntity<String> response) {
        if (response == null || response.getBody() == null) {
            return new ChatMessage(Role.ASSISTANT, "");
        }

        try {
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            JsonNode choices = responseNode.get("choices");
            String responseMessage = StreamSupport.stream(choices.spliterator(), false)
                    .map(choice -> choice.path("message").path("content").asText())
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining());
            return new ChatMessage(Role.ASSISTANT, responseMessage);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse model response", e);
        }
    }

}
