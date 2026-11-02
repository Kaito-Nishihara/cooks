package app.recipebook.auth.account.presentation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 利用者の識別子(UUID)を「,」で区切って並べた入力を、4.8 の決まりで確かめる。
 * 1 件以上、100 件以下で、どれも UUID の形である。区切り方は UserIdListText にある。
 */
@Constraint(validatedBy = UserIdListValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserIdList {

    String message() default "利用者の識別子の並びに誤りがあります。";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
