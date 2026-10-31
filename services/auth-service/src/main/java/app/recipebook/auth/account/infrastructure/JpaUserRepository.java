package app.recipebook.auth.account.infrastructure;

import app.recipebook.auth.account.domain.EmailAddress;
import app.recipebook.auth.account.domain.User;
import app.recipebook.auth.account.domain.UserRepository;
import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Map;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;

/** 利用者のリポジトリの実装。 */
@ApplicationScoped
public class JpaUserRepository implements UserRepository {

    private static final String UNIQUE_EMAIL = "uq_users_email";

    @Inject
    EntityManager entityManager;

    @Override
    public void add(User user) {
        try {
            entityManager.persist(UserJpaEntity.from(user));
            // 重複を、ここで見つける。先に検索して確かめる形にしない。同時に 2 つ届いても、1 つだけが成功する
            entityManager.flush();
        } catch (ConstraintViolationException e) {
            if (UNIQUE_EMAIL.equals(e.getConstraintName())) {
                throw new AppException(ErrorCode.EMAIL_TAKEN, Map.of(), e);
            }
            throw e;
        }
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return entityManager
                .createQuery("SELECT u FROM UserJpaEntity u WHERE u.email = :email", UserJpaEntity.class)
                .setParameter("email", email.value())
                .getResultStream()
                .findFirst()
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(entityManager.find(UserJpaEntity.class, id.value())).map(UserJpaEntity::toDomain);
    }

    @Override
    public void lock(UserId id) {
        entityManager.find(UserJpaEntity.class, id.value(), LockModeType.PESSIMISTIC_WRITE);
    }
}
