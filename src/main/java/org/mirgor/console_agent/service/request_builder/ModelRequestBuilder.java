package org.mirgor.console_agent.service.request_builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.mirgor.console_agent.service.model.Model;
import org.mirgor.console_agent.service.model.ChatMessage;
import org.mirgor.console_agent.service.model.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public abstract class ModelRequestBuilder {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    public abstract List<Model> getModels();

    public abstract ChatMessage getSystemPromptMessage(String prompt);

    public abstract String buildRequestBody(Model model, List<ChatMessage> context) throws JsonProcessingException;

    public abstract ChatMessage parseResponse(ResponseEntity<String> response);

    public HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", apiKey));
        return httpHeaders;
    }
}
