package com.employeehub.ai;

import com.employeehub.query.EmployeeQuery;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionInterpretationTest {

    @Test
    void convertsToEmployeeQuery() {
        QuestionInterpretation i = new QuestionInterpretation();
        i.setAnswerable(true);
        i.setDepartment("Sales");
        i.setHiredOnOrAfter("2026-07-13");
        i.setResultType("count");
        i.setSortBy("HIRE_DATE");
        i.setSortDirection("desc");

        EmployeeQuery q = i.toEmployeeQuery();

        assertThat(q.getDepartment()).isEqualTo("Sales");
        assertThat(q.getHiredOnOrAfter()).isEqualTo(LocalDate.of(2026, 7, 13));
        assertThat(q.getResultType()).isEqualTo(EmployeeQuery.ResultType.COUNT);
        assertThat(q.getSortBy()).isEqualTo(EmployeeQuery.SortField.HIRE_DATE);
        assertThat(q.getSortDirection()).isEqualTo(EmployeeQuery.SortDirection.DESC);
    }

    @Test
    void blankValuesMeanNoFilter() {
        QuestionInterpretation i = new QuestionInterpretation();
        i.setHiredOnOrAfter("");
        i.setResultType(" ");

        EmployeeQuery q = i.toEmployeeQuery();

        assertThat(q.getHiredOnOrAfter()).isNull();
        assertThat(q.getResultType()).isNull(); // validator fills in the default
    }

    @Test
    void malformedDateOrEnumIsAnInvalidResponse() {
        QuestionInterpretation badDate = new QuestionInterpretation();
        badDate.setHiredOnOrAfter("last March");
        assertThatThrownBy(badDate::toEmployeeQuery)
                .isInstanceOf(AiServiceException.class)
                .extracting(e -> ((AiServiceException) e).getKind())
                .isEqualTo(AiServiceException.Kind.INVALID_RESPONSE);

        QuestionInterpretation badEnum = new QuestionInterpretation();
        badEnum.setSortBy("AGE");
        assertThatThrownBy(badEnum::toEmployeeQuery)
                .extracting(e -> ((AiServiceException) e).getKind())
                .isEqualTo(AiServiceException.Kind.INVALID_RESPONSE);
    }
}
