package app.salescopilot.importing;

import java.util.UUID;

import org.springframework.stereotype.Component;

import app.salescopilot.importing.domain.ExtractedPlaybook;
import app.salescopilot.importing.domain.ImportMerge;
import app.salescopilot.playbook.api.PlaybookDrafts;
import app.salescopilot.shared.Tenancy;

/** Processa tarefas de importação (AD-032, AD-038). */
@Component
class ImportWorker {

    static final int MAX_TEXT = 100_000;

    private final ImportJobs jobs;
    private final ImportFiles files;
    private final DocumentText documents;
    private final PlaybookExtraction extraction;
    private final PlaybookDrafts drafts;
    private final Tenancy tenancy;

    ImportWorker(ImportJobs jobs, ImportFiles files, DocumentText documents, PlaybookExtraction extraction,
            PlaybookDrafts drafts, Tenancy tenancy) {
        this.jobs = jobs;
        this.files = files;
        this.documents = documents;
        this.extraction = extraction;
        this.drafts = drafts;
        this.tenancy = tenancy;
    }

    /** Processa tarefas até a fila esvaziar. */
    void drain() {
        for (var claim = jobs.claim(); claim.isPresent(); claim = jobs.claim()) {
            process(claim.get());
        }
    }

    void process(ImportJobs.Claim claim) {
        var job = tenancy.inBusiness(null, claim.businessId(), () -> jobs.find(claim.jobId())).orElseThrow();
        boolean finished;
        try {
            DocumentText.Extracted text = read(job);
            // O limite é sobre o texto do documento; o XHTML que vai ao modelo é maior por causa das tags.
            if (text.plain().length() > MAX_TEXT) {
                throw new ImportFailure("text_too_long");
            }
            ExtractedPlaybook extracted = extraction.extract(text.forModel());
            if (extracted.isEmpty()) {
                throw new ImportFailure("nothing_found");
            }
            finished = complete(job, claim.leaseToken(), extracted, text.plain());
        } catch (ImportFailure failure) {
            finished = fail(job, claim.leaseToken(), failure.code());
        } catch (RuntimeException | Error unexpected) {
            // Sem isto a tarefa voltaria à fila a cada concessão vencida, sem fim. Error entra porque um
            // documento malicioso pode esgotar a memória do parser; o que ele alocou já foi liberado aqui.
            finished = fail(job, claim.leaseToken(), "import_failed");
        }
        // Execução sem a concessão não apaga nada: o arquivo pertence a quem a assumiu.
        if (finished && job.filePath() != null) {
            files.delete(job.filePath());
        }
    }

    /**
     * Linhas, revisão nova e estado final na mesma transação, com o playbook travado (AD-038, AD-050).
     * Se a tarefa não puder ser marcada, tudo é desfeito.
     */
    boolean complete(ImportJobs.Job job, UUID leaseToken, ExtractedPlaybook extracted, String plainText) {
        try {
            tenancy.runInBusiness(null, job.businessId(), () -> {
                var locked = drafts.lock(job.playbookId());
                var result = ImportMerge.merge(locked.draft(), extracted, plainText);
                drafts.replace(job.playbookId(), result.draft());
                if (jobs.finishDone(job.id(), leaseToken, result.counts(), result.skipped(), result.notices()) != 1) {
                    throw new LeaseLost();
                }
            });
            return true;
        } catch (LeaseLost e) {
            return false;
        }
    }

    private boolean fail(ImportJobs.Job job, UUID leaseToken, String code) {
        return tenancy.inBusiness(null, job.businessId(), () -> jobs.finishFailed(job.id(), leaseToken, code) == 1);
    }

    private DocumentText.Extracted read(ImportJobs.Job job) throws ImportFailure {
        return switch (job.sourceKind()) {
            case "google_docs" -> documents.fromGoogleDocs(job.sourceUrl());
            default -> documents.fromFile(files.resolve(job.filePath()));
        };
    }

    private static final class LeaseLost extends RuntimeException {
    }
}
