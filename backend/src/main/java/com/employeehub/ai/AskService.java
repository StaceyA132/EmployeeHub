package com.employeehub.ai;

import com.employeehub.dto.AskResponse;
import com.employeehub.dto.EmployeeSearchResult;
import com.employeehub.model.Department;
import com.employeehub.query.EmployeeQuery;
import com.employeehub.query.EmployeeQueryValidator;
import com.employeehub.query.InvalidQueryException;
import com.employeehub.repository.DepartmentRepository;
import com.employeehub.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Answers plain-English questions: the AI turns the question into filters, then this service validates
 * them and runs the search with the existing repository code. The AI never sees employee records.
 */
@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final AiQueryInterpreter interpreter;
    private final EmployeeQueryValidator validator;
    private final EmployeeService employeeService;
    private final DepartmentRepository departmentRepository;

    public AskService(AiQueryInterpreter interpreter, EmployeeQueryValidator validator, EmployeeService employeeService,
                      DepartmentRepository departmentRepository) {
        this.interpreter = interpreter;
        this.validator = validator;
        this.employeeService = employeeService;
        this.departmentRepository = departmentRepository;
    }

    /**
     * @throws AiServiceException if the AI provider fails, times out, or twice returns unusable output
     */
    public AskResponse ask(String question, boolean canViewSalary) {
        List<String> departments = departmentRepository.findAll().stream().map(Department::getName).sorted().toList();
        InterpretationContext context = new InterpretationContext(LocalDate.now(), departments, canViewSalary);

        QuestionInterpretation interpretation = interpretWithOneRetry(question, context);
        if (!interpretation.isAnswerable()) {
            String reason = interpretation.getReason();
            return AskResponse.cannotAnswer(reason == null || reason.isBlank()
                    ? "I can't answer that from the employee directory." : reason);
        }
        EmployeeQuery query = interpretation.toEmployeeQuery();

        try {
            validator.validate(query, canViewSalary);
        } catch (InvalidQueryException e) {
            return AskResponse.cannotAnswer(e.getMessage());
        }

        EmployeeSearchResult result = employeeService.search(query);
        if (!canViewSalary) {
            result.employees().forEach(dto -> dto.setSalary(null));
        }
        return new AskResponse(true, AnswerFormatter.answer(query, result), result.totalMatches(),
                result.employees(), AnswerFormatter.interpretedAs(query));
    }

    // Models occasionally return malformed output; one retry usually fixes it. Other failures aren't retried
    // here because the SDK already retries timeouts, 429s, and 5xx errors.
    private QuestionInterpretation interpretWithOneRetry(String question, InterpretationContext context) {
        try {
            return checked(interpreter.interpret(question, context));
        } catch (AiServiceException e) {
            if (e.getKind() != AiServiceException.Kind.INVALID_RESPONSE) {
                throw e;
            }
            log.warn("AI returned an unusable interpretation, retrying once: {}", e.getMessage());
            return checked(interpreter.interpret(question, context));
        }
    }

    // Parse dates/enums/amounts now so a malformed value also triggers the retry
    private static QuestionInterpretation checked(QuestionInterpretation interpretation) {
        if (interpretation.isAnswerable()) {
            interpretation.toEmployeeQuery();
        }
        return interpretation;
    }
}
