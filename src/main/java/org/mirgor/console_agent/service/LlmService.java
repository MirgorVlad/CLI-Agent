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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static final String SYSTEM_PROMPT_PATH = "/system_prompt.txt";
    private static Path PROJECT_DIR_PATH;
    private Model model = Model.GPT_4_1;
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

        PROJECT_DIR_PATH = Paths.get(System.getProperty("user.dir"), "src");
    }

    private void initSystemPrompt() throws IOException {
        Path path = Paths.get(Utils.class.getResource(SYSTEM_PROMPT_PATH).getPath());
        String systemPrompt = Utils.getFileContents(path);
        context.add(new ChatMessage(Role.DEVELOPER, systemPrompt));
    }

    public String sendUserPrompt(String prompt) throws JsonProcessingException {
        String extendPrompt = extendPromptWithInputFiles(prompt);
        ChatMessage userMessage = new ChatMessage(Role.USER, extendPrompt);
        context.add(userMessage);

        ModelRequestBuilder modelRequestBuilder = modelRequestBuilderMap.get(model);
        LlmConfig.LlmDetails llmDetails = llmConfig.get(model);
        HttpHeaders headers = modelRequestBuilder.buildHeaders(llmDetails.key());
        String requestBody = modelRequestBuilder.buildRequestBody(model, context);

        ChatMessage modelResponse = sendRequest(llmDetails.endpoint(), headers, requestBody, modelRequestBuilder::parseResponse);
        context.add(modelResponse);

        return modelResponse.content();
    }

    private String extendPromptWithInputFiles(String prompt) {
        StringBuilder sb = new StringBuilder(prompt);
        List<String> fileNamesFromPrompt = findFileNamesFromPrompt(prompt);
        fileNamesFromPrompt.forEach(name -> {
            List<File> files = Utils.findFilesByName(PROJECT_DIR_PATH, name);
            String filesContents = files.stream()
                    .map(file -> {
                        try {
                            String fileContents = Utils.getFileContents(file.toPath());
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
        Pattern pattern = Pattern.compile("#files\\[(.*?)]");
        Matcher matcher = pattern.matcher(prompt);
        List<String> allFiles = new ArrayList<>();
        while (matcher.find()) {
            String files = matcher.group(1);
            allFiles.addAll(Arrays.asList(files.split(",")));
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

    public Model getCurrentModel() {
        return model;
    }

    public void setCurrentModel(Model model) {
        this.model = model;
    }

}
