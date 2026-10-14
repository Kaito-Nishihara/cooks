package app.recipebook.auth.account.application;

import app.recipebook.auth.session.domain.SessionToken;
import java.util.Optional;

/**
 * 登録の入力。
 *
 * @param currentSession 要求に付いていたクッキーのトークン。なければ空
 */
public record RegisterCommand(String displayName, String email, String password,
        Optional<SessionToken> currentSession) {

    @Override
    public String toString() {
        return "RegisterCommand[隠した値]";
    }
}
