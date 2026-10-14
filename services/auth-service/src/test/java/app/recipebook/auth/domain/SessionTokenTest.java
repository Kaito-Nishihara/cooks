package app.recipebook.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.recipebook.auth.session.domain.SessionToken;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** セッションのトークンの決まり(「共通設計: 認証」の 4.3)。 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SessionTokenTest {

    private static final String VALID = "A".repeat(43);

    @Test
    void トークンは_43_文字の_URL_に使える文字だけを受け付ける() {
        assertThat(SessionToken.parse(VALID)).isPresent();
        assertThat(SessionToken.parse("A".repeat(42))).isEmpty();
        assertThat(SessionToken.parse("A".repeat(42) + "+")).isEmpty();
        assertThat(SessionToken.parse(null)).isEmpty();
        assertThatThrownBy(() -> new SessionToken("短い")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void トークンのハッシュは_64_文字の_16_進で_もとの値を含まない() {
        String hash = new SessionToken(VALID).hash().value();

        assertThat(hash).matches("[0-9a-f]{64}").isNotEqualTo(VALID);
        assertThat(new SessionToken(VALID).hash()).isEqualTo(new SessionToken(VALID).hash());
    }

    @Test
    void トークンは_文字列に直しても中身を出さない() {
        assertThat(new SessionToken(VALID).toString()).doesNotContain(VALID);
    }
}
