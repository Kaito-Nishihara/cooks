package app.recipebook.auth.shared.presentation;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * この入口は、自分のサイトの画面から送られた要求だけを受ける(「共通設計: 認証」の 4.5)。
 * 入口が verify を呼ばない API(登録、ログイン)に付ける。ほかの API は、verify の中で確かめる。
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSameOrigin {
}
