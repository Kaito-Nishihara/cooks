package app.recipebook.common.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.List;

/**
 * 本文の JSON を読めなかったときの例外を、応答の fields に変える。
 * 例外の文には入力の値が入っていることがあるので、文は使わず、位置と種類だけを使う。
 */
final class JsonErrorTranslator {

    private JsonErrorTranslator() {
    }

    static List<FieldError> translate(JsonProcessingException exception) {
        if (exception instanceof UnrecognizedPropertyException unknown) {
            return List.of(FieldError.of(pathOf(unknown.getPath()), FieldErrorCode.UNKNOWN_FIELD));
        }
        if (exception instanceof MismatchedInputException mismatched && !mismatched.getPath().isEmpty()) {
            return List.of(FieldError.of(pathOf(mismatched.getPath()), FieldErrorCode.INVALID_FORMAT));
        }
        // JSON として読めない本文は、位置を示せないので、fields を空で返す
        return List.of();
    }

    /** 例外の原因をたどり、JSON の読み取りの例外があれば返す。 */
    static JsonProcessingException findIn(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof JsonProcessingException json) {
                return json;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return null;
    }

    private static String pathOf(List<JsonMappingException.Reference> references) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference reference : references) {
            if (reference.getFieldName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }
}
