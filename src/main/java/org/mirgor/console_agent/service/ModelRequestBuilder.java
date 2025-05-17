package org.mirgor.console_agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ModelRequestBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", apiKey));
        return httpHeaders;
    }

    public static String buildRequestBody(Model model, String prompt) throws JsonProcessingException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestBody.put("model", model.getLabel());
        requestBody.set("messages", messages);

        return objectMapper.writeValueAsString(requestBody);
    }

    public static String parseResponse(ResponseEntity<String> response) {
        if (response == null || response.getBody() == null) {
            return "";
        }

        try {
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            JsonNode choices = responseNode.get("choices");
            return StreamSupport.stream(choices.spliterator(), false)
                    .map(choice -> choice.path("message").path("content").asText())
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining());

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse model response", e);
        }
    }

}
