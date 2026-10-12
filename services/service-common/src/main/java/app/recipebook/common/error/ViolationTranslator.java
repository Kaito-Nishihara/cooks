package app.recipebook.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.validator.engine.HibernateConstraintViolation;

/** Bean Validation の違反を、応答の fields の 1 件に変える。 */
final class ViolationTranslator {

    private static final Set<Class<? extends Annotation>> REQUIRED =
            Set.of(NotNull.class, NotBlank.class, NotEmpty.class);
    private static final Set<Class<? extends Annotation>> MIN =
            Set.of(Min.class, DecimalMin.class, Positive.class, PositiveOrZero.class);
    private static final Set<Class<? extends Annotation>> MAX =
            Set.of(Max.class, DecimalMax.class, Negative.class, NegativeOrZero.class);

    private ViolationTranslator() {
    }

    /** 違反をすべて変える。応答が毎回同じ順になるように、位置の順に並べる。 */
    static List<FieldError> translate(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ViolationTranslator::translate)
                .sorted(Comparator.comparing(FieldError::path).thenComparing(FieldError::code))
                .toList();
    }

    private static FieldError translate(ConstraintViolation<?> violation) {
        String path = pathOf(violation.getPropertyPath());
        // 機能が自分で種類と文を決めた違反(FieldViolation で知らせたもの)は、そのまま使う
        Object reported = violation.unwrap(HibernateConstraintViolation.class)
                .getDynamicPayload(FieldErrorCode.class);
        if (reported instanceof FieldErrorCode code) {
            return FieldError.of(path, code, violation.getMessage());
        }
        FieldErrorCode code = codeOf(violation);
        // 機能が文を決めていない違反(文が既定の鍵のまま)には、種類ごとの文を使う
        boolean hasOwnMessage = !violation.getMessageTemplate().startsWith("{");
        return hasOwnMessage ? FieldError.of(path, code, violation.getMessage()) : FieldError.of(path, code);
    }

    private static FieldErrorCode codeOf(ConstraintViolation<?> violation) {
        Annotation annotation = violation.getConstraintDescriptor().getAnnotation();
        Class<? extends Annotation> type = annotation.annotationType();
        if (REQUIRED.contains(type)) {
            return FieldErrorCode.REQUIRED;
        }
        if (MIN.contains(type)) {
            return FieldErrorCode.MIN;
        }
        if (MAX.contains(type)) {
            return FieldErrorCode.MAX;
        }
        if (annotation instanceof Size size) {
            return sizeCodeOf(size, violation.getInvalidValue());
        }
        return FieldErrorCode.INVALID_FORMAT;
    }

    /** 長さと件数の違反を、足りないのか、多いのかで分ける。 */
    private static FieldErrorCode sizeCodeOf(Size size, Object value) {
        boolean isText = value instanceof CharSequence;
        int actual = lengthOf(value);
        if (actual < size.min()) {
            return FieldErrorCode.TOO_SHORT;
        }
        return isText ? FieldErrorCode.TOO_LONG : FieldErrorCode.TOO_MANY;
    }

    private static int lengthOf(Object value) {
        if (value instanceof CharSequence text) {
            return text.length();
        }
        if (value instanceof Collection<?> collection) {
            return collection.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        if (value != null && value.getClass().isArray()) {
            return Array.getLength(value);
        }
        return 0;
    }

    /**
     * 入力の JSON の中の位置を、項目名と添字で書く。例: rows[2].amount
     * メソッドと引数の名前は、入力の中の位置ではないので除く。
     * 引数そのものの違反(クエリの変数など)は、引数の名前を返す。
     */
    private static String pathOf(Path propertyPath) {
        StringBuilder path = new StringBuilder();
        String parameterName = "";
        for (Path.Node node : propertyPath) {
            ElementKind kind = node.getKind();
            if (kind == ElementKind.PARAMETER) {
                parameterName = node.getName();
                continue;
            }
            if (node.isInIterable()) {
                Object position = node.getIndex() != null ? node.getIndex() : node.getKey();
                if (position != null) {
                    path.append('[').append(position).append(']');
                }
            }
            if (kind == ElementKind.PROPERTY) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(node.getName());
            }
        }
        return path.isEmpty() ? parameterName : path.toString();
    }
}
