package app.recipebook.common.validation;

import app.recipebook.common.error.FieldErrorCode;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

/**
 * 機能が作る検証(ConstraintValidator)から、誤りの種類と文を知らせるための道具。
 * 知らせた種類と文は、応答の fields にそのまま入る。
 */
public final class FieldViolation {

    private FieldViolation() {
    }

    /** 誤りを 1 件知らせる。検証の isValid は、この後に false を返す。 */
    public static void report(ConstraintValidatorContext context, FieldErrorCode code, String message) {
        HibernateConstraintValidatorContext hibernateContext =
                context.unwrap(HibernateConstraintValidatorContext.class);
        hibernateContext.disableDefaultConstraintViolation();
        hibernateContext.withDynamicPayload(code);
        // 文を、式として解釈させない。「{」や「$」を含む文でも、そのまま出す
        hibernateContext.addMessageParameter("text", message)
                .buildConstraintViolationWithTemplate("{text}")
                .addConstraintViolation();
    }
}
