package app.recipebook.auth.session.application;

import app.recipebook.auth.session.domain.SessionToken;
import java.util.Optional;

/**
 * ログインの入力。
 *
 * @param currentSession 要求に付いていたクッキーのトークン。なければ空
 */
public record LoginCommand(String email, String password, Optional<SessionToken> currentSession) {

    @Override
    public String toString() {
        return "LoginCommand[隠した値]";
    }
}
