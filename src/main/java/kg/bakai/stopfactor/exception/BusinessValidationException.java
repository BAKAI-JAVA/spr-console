package kg.bakai.stopfactor.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class BusinessValidationException extends RuntimeException {
    private final List<String> errors;

    public BusinessValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public BusinessValidationException(String error) {
        this(List.of(error));
    }
}
