package app.recipebook.auth.account.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import app.recipebook.auth.account.domain.PasswordHash;
import app.recipebook.auth.account.domain.RawPassword;
import app.recipebook.auth.config.AuthSettings;
import io.smallrye.config.WithDefault;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** bcrypt の計算(「共通設計: 認証」の 4.2)。Quarkus を起動しない。 */
class BcryptPasswordHasherTest {

    private static BcryptPasswordHasher hasherWithCost(int cost) {
        return new BcryptPasswordHasher(cost);
    }

    @Test
    @DisplayName("コストの既定の値は 12 である")
    void defaultCostIsTwelve() throws Exception {
        assertThat(AuthSettings.class.getMethod("bcryptCost").getAnnotation(WithDefault.class).value())
                .isEqualTo("12");
    }

    @Test
    @DisplayName("コスト 12 のハッシュは、$2 で始まる 60 文字で、コストが書き込まれている")
    void hashHasCost() {
        RawPassword password = RawPassword.forNew("ながいあいことばをつかう2026");

        PasswordHash hash = hasherWithCost(12).hash(password);

        assertThat(hash.value()).hasSize(60).startsWith("$2").contains("$12$");
    }

    @Test
    @DisplayName("同じパスワードでも、ハッシュは呼ぶたびに違い、どちらも照合に通る")
    void hashIsSaltedAndVerifiable() {
        BcryptPasswordHasher hasher = hasherWithCost(4);
        RawPassword password = RawPassword.forNew("ながいあいことばをつかう2026");

        PasswordHash first = hasher.hash(password);
        PasswordHash second = hasher.hash(password);

        assertThat(first.value()).isNotEqualTo(second.value());
        assertThat(hasher.matches(password, first)).isTrue();
        assertThat(hasher.matches(password, second)).isTrue();
        assertThat(hasher.matches(RawPassword.forMatching("ちがうあいことば"), first)).isFalse();
    }
}
