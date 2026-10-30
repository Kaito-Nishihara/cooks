package app.recipebook.common.testing.fixture.bad.account.presentation;

import app.recipebook.common.testing.fixture.bad.account.domain.Thing;
import app.recipebook.common.testing.fixture.bad.account.domain.ThingRepository;

/** 決まりを破っている見本。入口が、ドメインのリポジトリを直接使っている。 */
public class ThingEndpoint {

    private final ThingRepository repository;

    public ThingEndpoint(ThingRepository repository) {
        this.repository = repository;
    }

    public Thing get(String name) {
        return repository.find(name);
    }
}
