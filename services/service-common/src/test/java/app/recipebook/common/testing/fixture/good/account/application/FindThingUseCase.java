package app.recipebook.common.testing.fixture.good.account.application;

import app.recipebook.common.testing.fixture.good.account.domain.Thing;
import app.recipebook.common.testing.fixture.good.account.domain.ThingRepository;
import java.util.Optional;

/** 層の決まりを守っている、ユースケースの見本。 */
public class FindThingUseCase {

    private final ThingRepository repository;

    public FindThingUseCase(ThingRepository repository) {
        this.repository = repository;
    }

    public Optional<String> execute(String name) {
        return repository.find(name).map(Thing::name);
    }
}
