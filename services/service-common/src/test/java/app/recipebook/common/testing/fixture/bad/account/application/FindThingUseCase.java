package app.recipebook.common.testing.fixture.bad.account.application;

import app.recipebook.common.testing.fixture.bad.account.domain.Thing;
import app.recipebook.common.testing.fixture.bad.account.infrastructure.MemoryThingRepository;

/** 決まりを破っている見本。ユースケースが、基盤のクラスを名前で呼んでいる。 */
public class FindThingUseCase {

    public Thing execute(String name) {
        return new MemoryThingRepository().find(name);
    }
}
