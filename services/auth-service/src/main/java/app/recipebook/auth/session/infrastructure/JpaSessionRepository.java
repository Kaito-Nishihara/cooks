package app.recipebook.auth.session.infrastructure;

import app.recipebook.auth.session.domain.Session;
import app.recipebook.auth.session.domain.SessionId;
import app.recipebook.auth.session.domain.SessionRepository;
import app.recipebook.auth.session.domain.TokenHash;
import app.recipebook.auth.shared.domain.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

/** セッションのリポジトリの実装。 */
@ApplicationScoped
public class JpaSessionRepository implements SessionRepository {

    @Inject
    EntityManager entityManager;

    @Override
    public void add(Session session) {
        entityManager.persist(SessionJpaEntity.from(session));
    }

    @Override
    public List<Session> listByUser(UserId userId) {
        return entityManager
                .createQuery("SELECT s FROM SessionJpaEntity s WHERE s.userId = :userId", SessionJpaEntity.class)
                .setParameter("userId", userId.value())
                .getResultStream()
                .map(SessionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void delete(SessionId id) {
        entityManager.createQuery("DELETE FROM SessionJpaEntity s WHERE s.id = :id")
                .setParameter("id", id.value())
                .executeUpdate();
    }

    @Override
    public void deleteByTokenHash(TokenHash tokenHash) {
        entityManager.createQuery("DELETE FROM SessionJpaEntity s WHERE s.tokenHash = :tokenHash")
                .setParameter("tokenHash", tokenHash.value())
                .executeUpdate();
    }
}
