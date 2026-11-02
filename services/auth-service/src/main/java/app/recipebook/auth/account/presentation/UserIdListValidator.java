package app.recipebook.auth.account.presentation;

import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.common.error.FieldErrorCode;
import app.recipebook.common.validation.FieldViolation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

/** 識別子の並びを確かめる。誤りが複数あっても、ない、多すぎる、形が違う、の順で最初の 1 件だけを返す。 */
public class UserIdListValidator implements ConstraintValidator<UserIdList, String> {

    @Override
    public boolean isValid(String input, ConstraintValidatorContext context) {
        if (input == null || input.isBlank()) {
            FieldViolation.report(context, FieldErrorCode.REQUIRED, "利用者の識別子を 1 件以上入れてください。");
            return false;
        }
        List<String> ids = UserIdListText.split(input);
        if (ids.size() > UserIdListText.MAX_COUNT) {
            FieldViolation.report(context, FieldErrorCode.TOO_MANY,
                    "利用者の識別子は " + UserIdListText.MAX_COUNT + " 件までにしてください。");
            return false;
        }
        if (!ids.stream().allMatch(UserIdListValidator::isUuid)) {
            FieldViolation.report(context, FieldErrorCode.INVALID_FORMAT, "利用者の識別子は UUID の形で入れてください。");
            return false;
        }
        return true;
    }

    private static boolean isUuid(String text) {
        try {
            // UUID.fromString は、桁の足りない「1-2-3-4-5」も受け付けるので、文字列に戻して比べる
            return UserId.of(text).toString().equalsIgnoreCase(text);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
