package app.recipebook.auth.account.presentation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示名、メールアドレス、パスワードの入力を、4.1 の決まりで確かめる。
 * 1 つの項目に誤りが複数あっても、最初に当てはまる 1 件だけを返す。
 */
@Constraint(validatedBy = AccountInputValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccountInput {

    AccountInputKind value();

    String message() default "入力に誤りがあります。";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
