package org.mirgor.console_agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mirgor.console_agent.service.model.Model;
import org.mirgor.console_agent.service.model.ChatMessage;
import org.mirgor.console_agent.service.model.Role;
import org.mirgor.console_agent.service.request_builder.ModelRequestBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    public static final String SYSTEM_PROMPT_PATH = "/system_prompt.txt";

    private WebClient webClient;
    private Map<Model, ModelRequestBuilder> modelRequestBuilderMap;
    private List<ChatMessage> context;

    private final List<ModelRequestBuilder> modelRequestBuilderList;
    private final LlmConfig llmConfig;

    @PostConstruct
    public void init() throws IOException {
        try {
            this.webClient = WebClient.create();
        } catch (Exception e) {
            log.error("Can't initialize llm service{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        modelRequestBuilderMap = modelRequestBuilderList.stream()
                .flatMap(builder -> builder.getModels().stream()
                        .map(model -> Map.entry(model, builder)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        context = new ArrayList<>();
        initSystemPrompt();
    }

    private void initSystemPrompt() throws IOException {
        String systemPrompt = new String(Files.readAllBytes(Paths.get(getClass().getResource(SYSTEM_PROMPT_PATH).getPath())));
        context.add(new ChatMessage(Role.DEVELOPER, systemPrompt));
    }

    public String sendUserPrompt(Model model, String prompt) throws JsonProcessingException {
        ChatMessage userMessage = new ChatMessage(Role.USER, prompt);
        context.add(userMessage);

        ModelRequestBuilder modelRequestBuilder = modelRequestBuilderMap.get(model);
        LlmConfig.LlmDetails llmDetails = llmConfig.get(model);
        HttpHeaders headers = modelRequestBuilder.buildHeaders(llmDetails.key());
        String requestBody = modelRequestBuilder.buildRequestBody(model, context);

        ChatMessage modelResponse = sendRequest(llmDetails.endpoint(), headers, requestBody, modelRequestBuilder::parseResponse);
        context.add(modelResponse);

        return modelResponse.content();
    }

    public void clearContext() {
        if (context.size() > 1) {
            context.subList(1, context.size()).clear();
        }
    }

    public List<ChatMessage> getChatContext() {
        return context;
    }

    public void addDeveloperContext(String prompt) {
        context.add(new ChatMessage(Role.DEVELOPER, prompt));
    }

    private ChatMessage sendRequest(String url,
                                    HttpHeaders headers,
                                    String requestBody,
                                    Function<ResponseEntity<String>, ChatMessage> parseResponseFunction) {
        ResponseEntity<String> response = webClient.post()
                .uri(url)
                .headers(h -> h.putAll(headers))
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(String.class)
                .block();

        return parseResponseFunction.apply(response);
    }

}
