package app.recipebook.auth.session.application;

/** 定期の処理で消した行の数。 */
public record CleanupResult(int sessions, int loginAttempts) {
}
