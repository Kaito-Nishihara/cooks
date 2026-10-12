package app.recipebook.common.testing.fixture.bad.account.infrastructure;

import app.recipebook.common.testing.fixture.bad.account.domain.Thing;
import app.recipebook.common.testing.fixture.bad.account.domain.ThingRepository;

/** 基盤の見本。 */
public class MemoryThingRepository implements ThingRepository {

    @Override
    public Thing find(String name) {
        return new Thing(name);
    }
}
