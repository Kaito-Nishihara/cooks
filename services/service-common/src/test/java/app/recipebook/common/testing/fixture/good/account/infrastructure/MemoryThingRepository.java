package app.recipebook.common.testing.fixture.good.account.infrastructure;

import app.recipebook.common.testing.fixture.good.account.domain.Thing;
import app.recipebook.common.testing.fixture.good.account.domain.ThingRepository;
import java.util.Optional;

/** 層の決まりを守っている、基盤の見本。 */
public class MemoryThingRepository implements ThingRepository {

    @Override
    public Optional<Thing> find(String name) {
        return Optional.of(new Thing(name));
    }
}
