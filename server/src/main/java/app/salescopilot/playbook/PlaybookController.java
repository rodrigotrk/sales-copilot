package app.salescopilot.playbook;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.salescopilot.business.api.BusinessAccess;
import app.salescopilot.playbook.api.ImportActivity;
import app.salescopilot.playbook.api.PlaybookDocument;
import app.salescopilot.playbook.domain.DraftRules;
import app.salescopilot.playbook.domain.PublishRules;
import app.salescopilot.shared.ApiException;
import app.salescopilot.shared.FieldProblem;
import app.salescopilot.shared.Tenancy;

@RestController
@RequestMapping("/api/businesses/{businessId}/playbook")
class PlaybookController {

    record VersionView(int number, Instant publishedAt) {
    }

    record PlaybookView(UUID playbookId, String name, PlaybookDocument draft, long revision, VersionView latestVersion,
            boolean hasUnpublishedChanges, boolean hasCalls, ImportActivity.ImportView importJob) {
    }

    record SaveDraft(UUID playbookId, PlaybookDocument draft, Long revision) {
    }

    record SavedDraft(PlaybookDocument draft, long revision, boolean hasUnpublishedChanges) {
    }

    record Publish(UUID playbookId, Long revision) {
    }

    private final Tenancy tenancy;
    private final PlaybookStore store;
    private final ImportActivity imports;

    PlaybookController(Tenancy tenancy, PlaybookStore store, ImportActivity imports) {
        this.tenancy = tenancy;
        this.store = store;
        this.imports = imports;
    }

    @GetMapping
    PlaybookView get(BusinessAccess access) {
        return tenancy.inBusiness(access.userId(), access.businessId(), () -> {
            var playbook = store.current().orElseThrow(ApiException::notFound);
            var latest = store.latestVersion(playbook.id());
            boolean unpublished = hasUnpublishedChanges(playbook.id(), playbook.draft());
            return new PlaybookView(playbook.id(), playbook.name(), playbook.draft(), playbook.revision(),
                    latest.map(v -> new VersionView(v.number(), v.publishedAt())).orElse(null), unpublished,
                    // Calls só passam a existir na parte 2.
                    false, imports.latest(playbook.id()).orElse(null));
        });
    }

    @PutMapping("/draft")
    SavedDraft save(BusinessAccess access, @RequestBody SaveDraft request) {
        access.requireGestor();
        return tenancy.inBusiness(access.userId(), access.businessId(), () -> {
            var playbook = lockCurrent(request.playbookId(), request.revision());
            List<FieldProblem> problems = DraftRules.validate(request.draft());
            if (!problems.isEmpty()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "invalid_fields", "Corrija os campos destacados.",
                        problems);
            }
            PlaybookDocument draft = DraftNormalizer.normalize(request.draft());
            long revision = store.updateDraft(playbook.id(), draft);
            return new SavedDraft(draft, revision, hasUnpublishedChanges(playbook.id(), draft));
        });
    }

    @PostMapping("/versions")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Integer> publish(BusinessAccess access, @RequestBody Publish request) {
        access.requireGestor();
        return tenancy.inBusiness(access.userId(), access.businessId(), () -> {
            var playbook = lockCurrent(request.playbookId(), request.revision());
            var latest = store.latestVersion(playbook.id());
            PlaybookDocument published = PublishRules.publishedForm(playbook.draft());
            if (latest.isPresent() && latest.get().content().equals(published)) {
                throw new ApiException(HttpStatus.CONFLICT, "nothing_to_publish", "Não há alterações para publicar.");
            }
            PublishRules.check(playbook.draft()).ifPresent(refusal -> {
                throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, refusal.code(), refusal.detail(),
                        refusal.itemIds().stream()
                                .map(id -> new FieldProblem(id, null, refusal.code(), refusal.detail()))
                                .toList());
            });
            int number = latest.map(v -> v.number() + 1).orElse(1);
            store.insertVersion(playbook, published, number, access.userId());
            return Map.of("number", number);
        });
    }

    private boolean hasUnpublishedChanges(UUID playbookId, PlaybookDocument draft) {
        PlaybookDocument published = PublishRules.publishedForm(draft);
        return store.latestVersion(playbookId).map(v -> !v.content().equals(published)).orElse(draft.hasItems());
    }

    /** A trava comum da AD-050: trava o playbook e só então confere revisão e importação ativa. */
    private PlaybookStore.Row lockCurrent(UUID playbookId, Long revision) {
        if (playbookId == null) {
            throw ApiException.notFound();
        }
        var playbook = store.lock(playbookId).orElseThrow(ApiException::notFound);
        if (revision == null || revision != playbook.revision()) {
            throw new ApiException(HttpStatus.CONFLICT, "revision_conflict",
                    "O playbook foi alterado em outra aba. Recarregue para continuar.");
        }
        if (imports.isActive(playbook.id())) {
            throw new ApiException(HttpStatus.CONFLICT, "import_running", "Há uma importação em andamento.");
        }
        return playbook;
    }
}
