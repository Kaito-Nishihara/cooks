package app.recipebook.auth.session.domain;

import java.util.Objects;
import java.util.UUID;

/** セッションの行を指す識別子。クッキーに入れる値とは別のものである。 */
public record SessionId(UUID value) {

    public SessionId {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
