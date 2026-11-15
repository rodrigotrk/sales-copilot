package app.salescopilot.business.api;

import java.util.UUID;

/** Publicado dentro da transação que cria o negócio; quem escuta grava na mesma transação. */
public record BusinessCreated(UUID businessId, String playbookName) {
}
