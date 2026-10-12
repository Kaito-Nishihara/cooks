package app.recipebook.common.testing.fixture.good.account.domain;

import java.util.Optional;

/** リポジトリの約束の見本。 */
public interface ThingRepository {

    Optional<Thing> find(String name);
}
