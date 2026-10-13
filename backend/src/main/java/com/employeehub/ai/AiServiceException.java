package com.employeehub.ai;

/** Anything that went wrong talking to the AI provider, classified so the API can return the right status. */
public class AiServiceException extends RuntimeException {

    public enum Kind {
        /** No API key configured, so the feature is switched off. */
        NOT_CONFIGURED,
        /** The provider didn't answer within the configured timeout. */
        TIMEOUT,
        /** The provider is rate limiting or overloaded. */
        RATE_LIMITED,
        /** Any other provider-side or network failure. */
        PROVIDER_ERROR,
        /** The provider answered, but not with valid, usable JSON. */
        INVALID_RESPONSE
    }

    private final Kind kind;

    public AiServiceException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public AiServiceException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
