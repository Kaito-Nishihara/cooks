package app.recipebook.auth.account.application;

import app.recipebook.auth.session.domain.SessionToken;
import app.recipebook.auth.shared.domain.UserId;
import java.util.Optional;

/**
 * パスワードの変更の入力。
 *
 * @param me トークンの利用者
 * @param currentSession 要求に付いていたクッキーのトークン。なければ空
 */
public record ChangePasswordCommand(UserId me, Optional<SessionToken> currentSession, String currentPassword,
        String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordCommand[隠した値]";
    }
}
