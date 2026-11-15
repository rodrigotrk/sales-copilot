package app.salescopilot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulesTest {

    @Test
    @DisplayName("[C90] módulos do servidor só usam o api de outros módulos")
    void modulesOnlyUseEachOthersApi() {
        ApplicationModules.of(ServerApplication.class).verify();
    }
}
