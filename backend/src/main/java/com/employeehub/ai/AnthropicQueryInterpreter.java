package com.employeehub.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.InternalServerException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;
import java.util.Map;
import java.util.stream.Collectors;

/** {@link AiQueryInterpreter} backed by Claude, using structured outputs to get schema-shaped JSON. */
public class AnthropicQueryInterpreter implements AiQueryInterpreter {

    private static final Logger log = LoggerFactory.getLogger(AnthropicQueryInterpreter.class);
    private static final JsonOutputFormat.Schema SCHEMA = toSchema(InterpretationPrompt.schema());

    private final AnthropicClient client;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AnthropicQueryInterpreter(AiProperties properties, ObjectMapper objectMapper) {
        this(AnthropicOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .timeout(properties.getTimeout())
                .maxRetries(properties.getMaxRetries())
                .build(), properties, objectMapper);
    }

    /** Lets tests point the client at a local fake server. */
    AnthropicQueryInterpreter(AnthropicClient client, AiProperties properties, ObjectMapper objectMapper) {
        this.client = client;
        this.properties = properties;
        // Unknown fields mean the model ignored the schema: treat as invalid rather than silently dropping them
        this.objectMapper = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public QuestionInterpretation interpret(String question, InterpretationContext context) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(properties.getModel())
                .maxTokens(16000L)
                .system(InterpretationPrompt.SYSTEM)
                .addUserMessage(InterpretationPrompt.userMessage(question, context))
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.of(properties.getEffort()))
                        .format(JsonOutputFormat.builder().schema(SCHEMA).build())
                        .build())
                // If a safety classifier declines, let the API retry on a suitable fallback model
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();

        Message response;
        try {
            response = client.messages().create(params);
        } catch (RuntimeException e) {
            throw translate(e);
        }
        return parse(response);
    }

    QuestionInterpretation parse(Message response) {
        StopReason stop = response.stopReason().orElse(null);
        if (StopReason.REFUSAL.equals(stop)) {
            QuestionInterpretation declined = new QuestionInterpretation();
            declined.setAnswerable(false);
            declined.setReason("I can't help with that question.");
            return declined;
        }
        if (StopReason.MAX_TOKENS.equals(stop)) {
            throw new AiServiceException(AiServiceException.Kind.INVALID_RESPONSE, "AI response was cut off.");
        }

        String json = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining());
        if (json.isBlank()) {
            throw new AiServiceException(AiServiceException.Kind.INVALID_RESPONSE, "AI returned no text.");
        }
        try {
            return objectMapper.readValue(json, QuestionInterpretation.class);
        } catch (JsonProcessingException e) {
            throw new AiServiceException(AiServiceException.Kind.INVALID_RESPONSE, "AI returned invalid JSON.", e);
        }
    }

    /** Maps SDK exceptions (most specific first) to our provider-neutral kinds. */
    static AiServiceException translate(RuntimeException e) {
        if (e instanceof RateLimitException) {
            return new AiServiceException(AiServiceException.Kind.RATE_LIMITED, "AI provider is rate limiting requests.", e);
        }
        if (e instanceof InternalServerException ise && ise.statusCode() == 529) {
            return new AiServiceException(AiServiceException.Kind.RATE_LIMITED, "AI provider is overloaded.", e);
        }
        if (e instanceof UnauthorizedException || e instanceof PermissionDeniedException) {
            log.error("AI provider rejected the API key; check ANTHROPIC_API_KEY.");
            return new AiServiceException(AiServiceException.Kind.PROVIDER_ERROR, "AI provider rejected the request.", e);
        }
        if (e instanceof BadRequestException) {
            log.error("AI provider rejected the request (check AI_MODEL / AI_EFFORT): {}", e.getMessage());
            return new AiServiceException(AiServiceException.Kind.PROVIDER_ERROR, "AI provider rejected the request.", e);
        }
        if (e instanceof AnthropicServiceException) {
            return new AiServiceException(AiServiceException.Kind.PROVIDER_ERROR, "AI provider returned an error.", e);
        }
        if (e instanceof AnthropicIoException) {
            return isTimeout(e)
                    ? new AiServiceException(AiServiceException.Kind.TIMEOUT, "AI provider timed out.", e)
                    : new AiServiceException(AiServiceException.Kind.PROVIDER_ERROR, "Couldn't reach the AI provider.", e);
        }
        if (e instanceof AnthropicInvalidDataException) {
            return new AiServiceException(AiServiceException.Kind.INVALID_RESPONSE, "AI provider sent an unreadable response.", e);
        }
        return new AiServiceException(AiServiceException.Kind.PROVIDER_ERROR, "Unexpected error calling the AI provider.", e);
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof InterruptedIOException) { // includes SocketTimeoutException
                return true;
            }
        }
        return false;
    }

    private static JsonOutputFormat.Schema toSchema(Map<String, Object> schema) {
        JsonOutputFormat.Schema.Builder builder = JsonOutputFormat.Schema.builder();
        schema.forEach((key, value) -> builder.putAdditionalProperty(key, JsonValue.from(value)));
        return builder.build();
    }
}
