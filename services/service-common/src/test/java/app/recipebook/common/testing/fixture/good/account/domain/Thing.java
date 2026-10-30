package app.recipebook.common.testing.fixture.good.account.domain;

import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;

/** 層の決まりを守っている、ドメインの見本。 */
public record Thing(String name) {

    public Thing {
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
