package com.employeehub.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AiConfigTest {

    @Autowired
    private AiQueryInterpreter interpreter;

    @Test
    void withoutApiKeyTheAppStartsAndAskReportsNotConfigured() {
        InterpretationContext context = new InterpretationContext(LocalDate.now(), List.of("Sales"), true);

        assertThatThrownBy(() -> interpreter.interpret("how many people are in sales?", context))
                .isInstanceOf(AiServiceException.class)
                .extracting(e -> ((AiServiceException) e).getKind())
                .isEqualTo(AiServiceException.Kind.NOT_CONFIGURED);
    }
}
