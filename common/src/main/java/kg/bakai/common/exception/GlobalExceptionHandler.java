package kg.bakai.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Pattern SEGMENT = Pattern.compile("(\\w+)(?:\\[(\\d+)])?");

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ProblemDetail handleBusinessValidation(BusinessValidationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
                "Изменения не сохранены: исправьте ошибки и повторите.");
        problem.setProperty("errors", e.getErrors());
        return problem;
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    public ProblemDetail handleDataIntegrity(RuntimeException e) {
        log.warn("Нарушение целостности данных", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Данные уже изменены другим действием. Обновите страницу и повторите.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Запрос не выполнен: исправьте ошибки и повторите.");
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> fieldName(error.getField()) + ": " + error.getDefaultMessage() + ".")
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException e,
                                                                          HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        String detail = message("missing." + e.getParameterName(),
                "Не указан обязательный параметр: " + fieldName(e.getParameterName()) + ".");
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail));
    }

    /**
     * A field as the console names it, from field.* in messages.properties: "rules[1].conditions[0].value" is
     * "Правило 2, условие 1, значение". A field without a name there is shown as it is.
     */
    private String fieldName(String path) {
        List<String> parts = new ArrayList<>();
        for (String segment : path.split("\\.")) {
            Matcher m = SEGMENT.matcher(segment);
            if (!m.matches()) {
                parts.add(segment);
                continue;
            }
            String label = message("field." + m.group(1), m.group(1));
            parts.add(m.group(2) != null ? label + " " + (Integer.parseInt(m.group(2)) + 1) : label);
        }
        String joined = String.join(", ", parts);
        return joined.isEmpty() ? joined : Character.toUpperCase(joined.charAt(0)) + joined.substring(1);
    }

    private String message(String code, String fallback) {
        MessageSource source = getMessageSource();
        return source != null ? source.getMessage(code, null, fallback, LocaleContextHolder.getLocale()) : fallback;
    }
}
