package com.employeehub.ai;

/**
 * Turns a plain-English question into structured filters. This is the only thing the rest of the app
 * knows about the AI, so switching provider means adding one implementation of this interface.
 */
public interface AiQueryInterpreter {

    /**
     * @throws AiServiceException if the provider can't be reached, times out, or returns unusable output
     */
    QuestionInterpretation interpret(String question, InterpretationContext context);
}
