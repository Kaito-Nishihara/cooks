package app.recipebook.common.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/**
 * JSON の読み取りの決まり(「共通設計: API の共通仕様」の 4.3)。
 * 入力に知らない項目があれば失敗させる。項目名の打ち間違いが、黙って無視されるのを防ぐためである。
 */
@Singleton
public class StrictJsonCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
