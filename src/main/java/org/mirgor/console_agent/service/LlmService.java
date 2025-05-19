package org.mirgor.console_agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mirgor.console_agent.service.config.LlmConfig;
import org.mirgor.console_agent.service.model.Model;
import org.mirgor.console_agent.service.model.ChatMessage;
import org.mirgor.console_agent.service.model.Role;
import org.mirgor.console_agent.service.request_builder.ModelRequestBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private static Path PROJECT_DIR_PATH;
    private Profile profile;
    private WebClient webClient;
    private Map<Model, ModelRequestBuilder> modelRequestBuilderMap;
    private List<ChatMessage> context;

    private final List<ModelRequestBuilder> modelRequestBuilderList;
    private final LlmConfig llmConfig;

    @PostConstruct
    public void init() throws IOException {
        initWebClient();
        initModelRequestBuilderMap();

        context = new ArrayList<>();
        setCurrentProfile(Profile.SIMPLE);
        PROJECT_DIR_PATH = Paths.get(System.getProperty("user.dir"));
    }

    private void initModelRequestBuilderMap() {
        modelRequestBuilderMap = modelRequestBuilderList.stream()
                .flatMap(builder -> builder.getModels().stream()
                        .map(model -> Map.entry(model, builder)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    private void initWebClient() {
        try {
            this.webClient = WebClient.create();
        } catch (Exception e) {
            log.error("Can't initialize llm service{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void initSystemPrompt() throws IOException {
        context.clear();

        if (this.profile == Profile.SIMPLE) {
            return;
        }
        try (InputStream in = Utils.class.getResourceAsStream(profile.getSystemPromptFile())) {
            String systemPrompt = Utils.getFileContents(in);
            ModelRequestBuilder modelRequestBuilder = modelRequestBuilderMap.get(this.profile.getModel());
            context.add(modelRequestBuilder.getSystemPromptMessage(systemPrompt));
        }
    }

    public String sendUserPrompt(String prompt) throws JsonProcessingException {
        String extendPrompt = extendPromptWithInputFiles(prompt);
        ChatMessage userMessage = new ChatMessage(Role.USER, extendPrompt);
        context.add(userMessage);
        Model model = this.profile.getModel();
        ModelRequestBuilder modelRequestBuilder = modelRequestBuilderMap.get(model);
        LlmConfig.LlmDetails llmDetails = llmConfig.get(model);
        HttpHeaders headers = modelRequestBuilder.buildHeaders(llmDetails.key());
        String requestBody = modelRequestBuilder.buildRequestBody(model, context);

        ChatMessage modelResponse = sendRequest(llmDetails.endpoint(), headers, requestBody, modelRequestBuilder::parseResponse);
        context.add(modelResponse);

        return modelResponse.content();
    }

    public double getContextSizeCapacityRate() {
        long modelContextWindowSize = this.profile.getModel().getContextWindowSize();
        int contextWindow = countContextTokens();
        return (double) contextWindow / modelContextWindowSize;
    }

    private String extendPromptWithInputFiles(String prompt) {
        StringBuilder sb = new StringBuilder(prompt);
        List<String> fileNamesFromPrompt = findFileNamesFromPrompt(prompt);
        fileNamesFromPrompt.forEach(name -> {
            List<File> files = Utils.findFilesByName(PROJECT_DIR_PATH, name);
            String filesContents = files.stream()
                    .map(file -> {
                        try (InputStream in = Files.newInputStream(file.toPath())) {
                            String fileContents = Utils.getFileContents(in);
                            return String.format("\n%s:\n%s\n", name, fileContents);
                        } catch (IOException e) {
                            log.error("Can't read file {}", file.getPath(), e);
                        }
                        return "";
                    })
                    .collect(Collectors.joining());
            sb.append(filesContents);
        });
        return sb.toString();
    }

    private static List<String> findFileNamesFromPrompt(String prompt) {
        Pattern pattern = Pattern.compile("#file ([^\\s]+)");
        Matcher matcher = pattern.matcher(prompt);
        List<String> allFiles = new ArrayList<>();
        while (matcher.find()) {
            allFiles.add(matcher.group(1));
        }
        return allFiles;
    }

    public int countContextTokens() {
        return context.stream()
                .map(ChatMessage::content)
                .map(Utils::countTokens)
                .mapToInt(Integer::intValue)
                .sum();
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

    public Profile getCurrentProfile() {
        return profile;
    }

    public void setCurrentProfile(Profile profile) throws IOException {
        this.profile = profile;
        initSystemPrompt();
    }

}
