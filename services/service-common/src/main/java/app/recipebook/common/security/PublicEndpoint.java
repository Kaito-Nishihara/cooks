package app.recipebook.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * トークンなしで呼べる入口に付ける印。
 * 付いていない入口は、すべて、有効なトークンを要る。付け忘れたときに、拒む側に倒れるようにするためである。
 * 例: 登録、ログイン、入口から呼ばれる verify、公開の鍵
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {
}
