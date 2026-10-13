package com.employeehub.ai;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the real Claude Java SDK against a tiny local HTTP server that imitates the Messages API,
 * so success and every failure mode are tested without an API key or network access.
 */
class AnthropicQueryInterpreterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final InterpretationContext CONTEXT =
            new InterpretationContext(LocalDate.of(2026, 10, 13), List.of("Engineering", "Sales"), true);

    private HttpServer server;
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private volatile int status;
    private volatile String responseBody;
    private volatile long delayMs;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private AnthropicQueryInterpreter interpreter(Duration timeout) {
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-key");
        properties.setTimeout(timeout);
        properties.setMaxRetries(0);
        var client = AnthropicOkHttpClient.builder()
                .apiKey("test-key")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .timeout(timeout)
                .maxRetries(0)
                .build();
        return new AnthropicQueryInterpreter(client, properties, MAPPER);
    }

    private AnthropicQueryInterpreter interpreter() {
        return interpreter(Duration.ofSeconds(5));
    }

    private void respondWithText(String text, String stopReason) throws Exception {
        status = 200;
        responseBody = MAPPER.writeValueAsString(MAPPER.createObjectNode()
                .put("id", "msg_test").put("type", "message").put("role", "assistant").put("model", "claude-opus-5-5")
                .put("stop_reason", stopReason).putNull("stop_sequence")
                .<com.fasterxml.jackson.databind.node.ObjectNode>set("content", MAPPER.createArrayNode()
                        .add(MAPPER.createObjectNode().put("type", "text").put("text", text)))
                .set("usage", MAPPER.createObjectNode().put("input_tokens", 100).put("output_tokens", 50)));
    }

    private void respondWithError(int httpStatus, String type) {
        status = httpStatus;
        responseBody = "{\"type\":\"error\",\"error\":{\"type\":\"" + type + "\",\"message\":\"test\"}}";
    }

    private static final String VALID_JSON = """
            {"answerable":true,"reason":"","resultType":"COUNT","department":"Sales","status":"",
             "positionContains":"","nameContains":"","hiredOnOrAfter":"","hiredOnOrBefore":"",
             "minSalary":"","maxSalary":"","sortBy":"NAME","sortDirection":"ASC","limit":20}
            """;

    private static AiServiceException.Kind kindOf(Throwable e) {
        return ((AiServiceException) e).getKind();
    }

    @Test
    void parsesStructuredResponseAndSendsSchemaModelAndContext() throws Exception {
        respondWithText(VALID_JSON, "end_turn");

        QuestionInterpretation result = interpreter().interpret("how many people are in sales?", CONTEXT);

        assertThat(result.isAnswerable()).isTrue();
        assertThat(result.getDepartment()).isEqualTo("Sales");
        assertThat(result.getResultType()).isEqualTo("COUNT");

        JsonNode sent = MAPPER.readTree(lastRequestBody.get());
        assertThat(sent.path("model").asText()).isEqualTo("claude-opus-5-5");
        assertThat(sent.path("output_config").path("effort").asText()).isEqualTo("low");
        assertThat(sent.path("output_config").path("format").path("type").asText()).isEqualTo("json_schema");
        assertThat(sent.path("output_config").path("format").path("schema").path("additionalProperties").asBoolean(true)).isFalse();
        String userText = sent.path("messages").get(0).path("content").toString();
        assertThat(userText).contains("2026-10-13").contains("Engineering, Sales").contains("how many people are in sales?");
    }

    @Test
    void invalidJsonFromModelIsInvalidResponse() throws Exception {
        respondWithText("Sure! There are 3 people in sales.", "end_turn");
        assertThatThrownBy(() -> interpreter().interpret("q", CONTEXT))
                .satisfies(e -> assertThat(kindOf(e)).isEqualTo(AiServiceException.Kind.INVALID_RESPONSE));
    }

    @Test
    void unexpectedFieldIsInvalidResponse() throws Exception {
        respondWithText(VALID_JSON.replace("\"limit\":20", "\"limit\":20,\"sql\":\"DROP TABLE employees\""), "end_turn");
        assertThatThrownBy(() -> interpreter().interpret("q", CONTEXT))
                .satisfies(e -> assertThat(kindOf(e)).isEqualTo(AiServiceException.Kind.INVALID_RESPONSE));
    }

    @Test
    void truncatedResponseIsInvalidResponse() throws Exception {
        respondWithText("{\"answerable\":tr", "max_tokens");
        assertThatThrownBy(() -> interpreter().interpret("q", CONTEXT))
                .satisfies(e -> assertThat(kindOf(e)).isEqualTo(AiServiceException.Kind.INVALID_RESPONSE));
    }

    @Test
    void refusalBecomesUnanswerable() throws Exception {
        respondWithText("", "refusal");
        QuestionInterpretation result = interpreter().interpret("q", CONTEXT);
        assertThat(result.isAnswerable()).isFalse();
        assertThat(result.getReason()).isNotBlank();
    }

    @Test
    void rateLimitAndOverloadAreRateLimited() {
        respondWithError(429, "rate_limit_error");
        assertThatThrownBy(() -> interpreter().interpret("q", CONTEXT))
                .satisfies(e -> assertThat(kindOf(e)).isEqualTo(AiServiceException.Kind.RATE_LIMITED));

        respondWithError(529, "overloaded_error");
        assertThatThrownBy(() -> interpreter().interpret("q", CONTEXT))
                .satisfies(e -> assertThat(kindOf(e)).isEqualTo(AiServiceException.Kind.RATE_LIMITED));
    }

    @Test
    void serverErrorAndBadKeyAreProviderErrors() {
        respondWithError(500, "api_error");
        assertThatThrownBy(() -> interpreter().interpret("q", CONTEXT))
                .satisfies(e -> assertThat(kindOf(e)).isEqualTo(AiServiceException.Kind.PROVIDER_ERROR));

        respondWithError(401, "authentication_error");
        assertThatThrownBy(() -> interpreter().interpret("q", CONTEXT))
                .satisfies(e -> assertThat(kindOf(e)).isEqualTo(AiServiceException.Kind.PROVIDER_ERROR));
    }

    @Test
    void slowProviderIsTimeout() throws Exception {
        respondWithText(VALID_JSON, "end_turn");
        delayMs = 1_500;
        assertThatThrownBy(() -> interpreter(Duration.ofMillis(300)).interpret("q", CONTEXT))
                .satisfies(e -> assertThat(kindOf(e)).isEqualTo(AiServiceException.Kind.TIMEOUT));
    }
}
