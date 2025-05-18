package org.mirgor.console_agent.service.request_builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.mirgor.console_agent.service.model.Model;
import org.mirgor.console_agent.service.model.ChatMessage;
import org.mirgor.console_agent.service.model.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j @Component
public class OpenAiRequestBuilder extends ModelRequestBuilder{

    @Override
    public List<Model> getModels() {
        return List.of(Model.GPT_4_1, Model.GPT_O3_MINI);
    }

    @Override
    public String buildRequestBody(Model model, List<ChatMessage> context) throws JsonProcessingException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode messages = objectMapper.valueToTree(context);
        requestBody.put("model", model.getLabel());
        requestBody.set("input", messages);

        return objectMapper.writeValueAsString(requestBody);
    }

    @Override
    public ChatMessage parseResponse(ResponseEntity<String> response) {
        if (response == null || response.getBody() == null) {
            return new ChatMessage(Role.ASSISTANT, "");
        }

        try {
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            JsonNode output = responseNode.get("output");
            String responseMessage = StreamSupport.stream(output.spliterator(), false)
                    .flatMap(content -> StreamSupport.stream(content.path("content").spliterator(), false))
                    .map(contentPart -> contentPart.path("text").asText())
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining());
            return new ChatMessage(Role.ASSISTANT, responseMessage);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse model response", e);
        }
    }

}
