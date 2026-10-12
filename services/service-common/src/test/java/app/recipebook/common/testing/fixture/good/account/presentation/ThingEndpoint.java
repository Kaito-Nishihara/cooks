package app.recipebook.common.testing.fixture.good.account.presentation;

import app.recipebook.common.testing.fixture.good.account.application.FindThingUseCase;

/** 層の決まりを守っている、入口の見本。 */
public class ThingEndpoint {

    private final FindThingUseCase useCase;

    public ThingEndpoint(FindThingUseCase useCase) {
        this.useCase = useCase;
    }

    public String get(String name) {
        return useCase.execute(name).orElse(null);
    }
}
