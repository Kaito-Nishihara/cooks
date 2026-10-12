package app.recipebook.auth.shared.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 利用者の識別子。
 * ほかの種類の識別子と取り違えたときに、コンパイルで分かるように、種類ごとの型で持つ。
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    /** 文字列から作る。UUID の形でなければ IllegalArgumentException を投げる。 */
    public static UserId of(String text) {
        return new UserId(UUID.fromString(text));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
