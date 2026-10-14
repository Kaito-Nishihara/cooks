package app.recipebook.auth.shared.presentation;

import app.recipebook.auth.config.AuthSettings;
import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Set;

/**
 * 要求の送り元(Origin)の決まり(「共通設計: 認証」の 4.5)。
 * 状態を変える要求は、自分のサイトの画面から送られたものだけを受ける。
 */
@ApplicationScoped
public class OriginPolicy {

    /** 状態を変えるメソッド */
    private static final Set<String> STATE_CHANGING = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Inject
    AuthSettings settings;

    /** 送り元が自分のサイトでなければ、CROSS_SITE_REQUEST を投げる。ヘッダーがないときも拒む。 */
    public void requireSameOrigin(String origin) {
        if (origin == null || !origin.equals(settings.webOrigin())) {
            throw new AppException(ErrorCode.CROSS_SITE_REQUEST);
        }
    }

    /** 状態を変えるメソッドのときだけ、送り元を確かめる。 */
    public void requireSameOriginIfStateChanging(String method, String origin) {
        if (STATE_CHANGING.contains(method)) {
            requireSameOrigin(origin);
        }
    }
}
