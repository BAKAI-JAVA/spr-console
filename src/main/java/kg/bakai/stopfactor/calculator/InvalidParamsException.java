package kg.bakai.stopfactor.calculator;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidParamsException extends RuntimeException {
    private final List<String> errors;

    public InvalidParamsException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }
}
