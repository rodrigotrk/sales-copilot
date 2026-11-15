package app.salescopilot.playbook.api;

import java.util.UUID;

import org.springframework.stereotype.Component;

import app.salescopilot.shared.ApiException;

/**
 * Acesso de outros módulos ao rascunho, sempre dentro da transação de quem chama (com o negócio já
 * definido pela Tenancy). `lock` trava a linha do playbook: é a trava comum da AD-050.
 */
@Component
public class PlaybookDrafts {

    public record LockedDraft(UUID playbookId, PlaybookDocument draft, long revision) {
    }

    private final app.salescopilot.playbook.PlaybookStore store;

    PlaybookDrafts(app.salescopilot.playbook.PlaybookStore store) {
        this.store = store;
    }

    public LockedDraft lock(UUID playbookId) {
        var row = store.lock(playbookId).orElseThrow(ApiException::notFound);
        return new LockedDraft(row.id(), row.draft(), row.revision());
    }

    public long replace(UUID playbookId, PlaybookDocument draft) {
        return store.updateDraft(playbookId, draft);
    }
}
