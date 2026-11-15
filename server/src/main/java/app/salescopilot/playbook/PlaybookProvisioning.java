package app.salescopilot.playbook;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import app.salescopilot.business.api.BusinessCreated;

/** Cria o playbook vazio na mesma transação que cria o negócio. */
@Component
class PlaybookProvisioning {

    private final PlaybookStore store;

    PlaybookProvisioning(PlaybookStore store) {
        this.store = store;
    }

    @EventListener
    void on(BusinessCreated event) {
        store.create(event.businessId(), event.playbookName());
    }
}
