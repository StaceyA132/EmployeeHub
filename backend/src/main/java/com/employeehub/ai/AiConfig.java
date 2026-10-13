package com.employeehub.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public AiQueryInterpreter aiQueryInterpreter(AiProperties properties, ObjectMapper objectMapper) {
        if (!properties.isConfigured()) {
            log.info("No AI API key set (ANTHROPIC_API_KEY); the Ask feature is disabled.");
            return (question, context) -> {
                throw new AiServiceException(AiServiceException.Kind.NOT_CONFIGURED,
                        "The Ask feature isn't set up on this server.");
            };
        }
        // To add a provider: implement AiQueryInterpreter and add a case here
        return switch (properties.getProvider().toLowerCase()) {
            case "anthropic" -> new AnthropicQueryInterpreter(properties, objectMapper);
            default -> throw new IllegalStateException("Unknown AI provider: " + properties.getProvider());
        };
    }
}
