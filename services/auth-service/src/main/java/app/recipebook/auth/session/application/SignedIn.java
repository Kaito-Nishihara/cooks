package app.recipebook.auth.session.application;

import app.recipebook.auth.account.application.UserView;
import app.recipebook.auth.session.domain.SessionToken;

/** ログインした状態になった結果。利用者の情報と、クッキーに入れるトークンを持つ。 */
public record SignedIn(UserView user, SessionToken token) {
}
