package org.mirgor.console_agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmWebClient {

    private static final int MAX_RESPONSE_SIZE = 1000000;
    private EventLoopGroup eventLoopGroup;
    private WebClient webClient;

    private final LlmConfig llmConfig;

    @PostConstruct
    public void init() {
        try {
            this.eventLoopGroup = new NioEventLoopGroup();
            HttpClient httpClient = HttpClient.create()
                    .runOn(eventLoopGroup)
                    .secure(t -> {
                        try {
                            t.sslContext(SslContextBuilder.forClient().build());
                        } catch (SSLException e) {
                            throw new RuntimeException(e);
                        }
                    });

            this.webClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .exchangeStrategies(ExchangeStrategies.builder()
                            .codecs(configurer -> configurer.defaultCodecs()
                                    .maxInMemorySize(MAX_RESPONSE_SIZE))
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Can't initialize llm client{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public String getModelResponse(Model model, String prompt) throws JsonProcessingException {
        LlmConfig.LlmDetails llmDetails = llmConfig.get(model);
        HttpHeaders headers = ModelRequestBuilder.buildHeaders(llmDetails.key());
        String requestBody = ModelRequestBuilder.buildRequestBody(model, prompt);

        ResponseEntity<String> response = webClient.post()
                .uri(llmDetails.endpoint())
                .headers(h -> h.putAll(headers))
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(String.class)
                .block();
        return ModelRequestBuilder.parseResponse(response);
    }

    @PreDestroy
    public void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully();
        }
    }

}
