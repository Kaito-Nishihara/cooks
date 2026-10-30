package app.recipebook.common.testing.fixture.bad.account.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;

/** 決まりを破っている見本。ドメインが、枠組みの注釈と Jackson を使っている。 */
public class Thing {

    @NotNull
    private final String name;

    public Thing(String name) {
        this.name = name;
    }

    public String toJson() throws Exception {
        return new ObjectMapper().writeValueAsString(name);
    }
}
