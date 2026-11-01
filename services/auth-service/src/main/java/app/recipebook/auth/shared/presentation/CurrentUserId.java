package app.recipebook.auth.shared.presentation;

import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.common.security.CurrentUser;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/** トークンから取り出した本人を、このサービスの利用者の識別子にして渡す。 */
@RequestScoped
public class CurrentUserId {

    @Inject
    CurrentUser currentUser;

    public UserId get() {
        return new UserId(currentUser.id());
    }
}
