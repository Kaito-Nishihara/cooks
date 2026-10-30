package app.recipebook.auth.account.presentation;

import app.recipebook.auth.shared.domain.InputProblem;
import app.recipebook.common.error.FieldErrorCode;
import app.recipebook.common.validation.FieldViolation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;

/** 値オブジェクトの決まりで入力を確かめ、合わない理由を、応答の誤りの種類と文に置き換える。 */
public class AccountInputValidator implements ConstraintValidator<AccountInput, String> {

    private AccountInputKind kind;

    @Override
    public void initialize(AccountInput annotation) {
        this.kind = annotation.value();
    }

    @Override
    public boolean isValid(String input, ConstraintValidatorContext context) {
        Optional<InputProblem> problem = kind.check(input);
        if (problem.isEmpty()) {
            return true;
        }
        FieldViolation.report(context, codeOf(problem.get()), kind.messageOf(problem.get()));
        return false;
    }

    private static FieldErrorCode codeOf(InputProblem problem) {
        return switch (problem) {
            case REQUIRED -> FieldErrorCode.REQUIRED;
            case INVALID_FORMAT -> FieldErrorCode.INVALID_FORMAT;
            case TOO_SHORT -> FieldErrorCode.TOO_SHORT;
            case TOO_LONG -> FieldErrorCode.TOO_LONG;
        };
    }
}
