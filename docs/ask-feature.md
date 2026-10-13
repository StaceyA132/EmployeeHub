# Ask: plain-English questions about employees

The **Ask** page lets any signed-in user ask questions like *"who joined in the last 3 months?"* or *"how many people are in Sales?"* and get answers from EmployeeHub's real data.

## How it works

```
"Who joined in the last 3 months?"
        │
        ▼
┌──────────────────────┐   question + today's date + department names
│ AskService (backend) │ ─────────────────────────────────────────────▶  Claude
│                      │ ◀─────────────────────────────────────────────  JSON filters
└──────────────────────┘   {"hiredOnOrAfter": "2026-07-13", "sortBy": "HIRE_DATE", ...}
        │
        ▼  validate filters → run a JPA Specification query → write the answer
"Found 3 employees hired on or after Jul 13, 2026."  + list + "Interpreted as" chips
```

1. **Interpret.** The question goes to Claude with a strict JSON schema. Claude returns *filters* (department, status, position, name, hire-date range, salary range, list vs count, sort, limit), never SQL and never an answer.
2. **Validate.** `EmployeeQueryValidator` checks every value: the department must exist, the status must be a real status, ranges must be the right way round, salary filters require an admin or manager, and the limit is capped at 50.
3. **Query.** `EmployeeSpecifications` turns the filters into a JPA query with bound parameters. It runs through the existing `EmployeeService` and repository.
4. **Answer.** `AnswerFormatter` writes the sentence from the real results. The UI also shows the filters that were used ("Interpreted as") so users can see what the AI understood.

## Safety and privacy

| Concern | How it's handled |
| --- | --- |
| SQL injection or unsafe queries | The model only fills in a fixed set of typed filters. Queries are JPA Specifications with bound parameters, and `LIKE` wildcards are escaped. |
| Employee data leaving the server | Only the question, today's date, department names, and whether the user may see salaries are sent. No names, emails, or salaries. |
| Made-up answers | The answer text is written by the backend from query results, not by the model. |
| Salary privacy | Read-only users can't filter or sort by salary (that would reveal salaries indirectly), and salaries are removed from their results. |
| Prompt injection ("ignore your instructions...") | The worst case is a different but still valid and permission-checked search. Unknown fields in the model's JSON are rejected. |
| Questions the data can't answer | The model returns `answerable: false` with a reason, which is shown as "Can't answer that". |

## Configuration

Set in `.env` (see `.env.example`). Only the API key is needed:

| Variable | Default | Purpose |
| --- | --- | --- |
| `ANTHROPIC_API_KEY` | *(blank)* | Turns the feature on. Blank = the app runs normally and Ask returns "not set up". |
| `AI_MODEL` | `claude-opus-5-5` | Claude model used to interpret questions. |
| `AI_EFFORT` | `low` | Reasoning effort. Interpreting a question is a simple task, so low keeps it fast. |
| `AI_TIMEOUT` | `20s` | Timeout per attempt. |
| `AI_MAX_RETRIES` | `2` | SDK retries for timeouts, rate limits, and server errors. |

`docker-compose.yml` passes `ANTHROPIC_API_KEY` to the backend. To change the others when using Docker, add them to the backend's `environment` section.

## API

`POST /api/ask` (any signed-in user)

```json
{ "question": "How many people are in Sales?" }
```

```json
{
  "answerable": true,
  "answer": "There are 2 employees in Sales.",
  "totalMatches": 2,
  "employees": [],
  "interpretedAs": ["Department: Sales", "Count only"]
}
```

| Status | When |
| --- | --- |
| `200` | Answered, or `answerable: false` with a reason |
| `400` | Question is blank or over 300 characters |
| `401` | Not signed in |
| `502` | The AI service failed, or twice returned output we couldn't use |
| `503` | Ask isn't configured, or the AI service is rate limiting |
| `504` | The AI service timed out |

## Switching providers

The app only depends on the `AiQueryInterpreter` interface (question in, `QuestionInterpretation` out). To use another provider:

1. Implement `AiQueryInterpreter`. Reuse `InterpretationPrompt.SYSTEM`, `userMessage(...)`, and `schema()`.
2. Translate that provider's errors into `AiServiceException` kinds.
3. Add a `case` for it in `AiConfig` and set `AI_PROVIDER`.

## Code map

| File | Role |
| --- | --- |
| `ai/AskService.java` | Orchestrates interpret → validate → query → answer; retries unusable model output once |
| `ai/AiQueryInterpreter.java` | Provider-neutral interface |
| `ai/AnthropicQueryInterpreter.java` | Claude implementation (structured outputs, error mapping) |
| `ai/InterpretationPrompt.java` | System prompt, per-question message, JSON schema |
| `ai/QuestionInterpretation.java` | Model output shape, safely converted to `EmployeeQuery` |
| `query/EmployeeQuery*.java`, `query/EmployeeSpecifications.java` | Filters, validation, and JPA query building |
| `controller/AskController.java` | `POST /api/ask` |
| `frontend/src/pages/AskPage.tsx` | Chat UI |

## Tests

`mvn test` covers this feature without an API key:

- `EmployeeQueryTest`: filtering, sorting, limits, permissions, validation
- `AnthropicQueryInterpreterTest`: the real Claude SDK against a local fake server (valid JSON, invalid JSON, extra fields, truncation, refusals, 429/529, 500, 401, timeouts)
- `AskServiceTest`: end to end with a mocked AI (answers, salary hiding, can't-answer messages, one retry on bad output)
- `AskControllerTest`: the HTTP endpoint, auth, validation, and status mapping
