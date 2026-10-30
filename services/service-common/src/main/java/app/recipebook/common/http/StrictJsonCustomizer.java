package app.recipebook.common.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/**
 * JSON の読み取りの決まり(「共通設計: API の共通仕様」の 4.3)。
 * 入力に知らない項目があれば失敗させる。項目名の打ち間違いが、黙って無視されるのを防ぐためである。
 * 型の違う値も、読み替えずに失敗させる。
 */
@Singleton
public class StrictJsonCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 文字列の項目に、数や真偽の値が来たときに、文字列へ読み替えない。型の違いとして拒む
        objectMapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
    }
}
