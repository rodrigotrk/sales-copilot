package app.salescopilot.importing;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * Apaga arquivos de tarefas encerradas e arquivos sem tarefa com mais de 1 hora (AD-038): cobre a
 * queda entre gravar o arquivo e gravar a tarefa, e a queda depois do commit do estado final.
 */
@Component
class ImportFileSweeper {

    static final Duration ORPHAN_AGE = Duration.ofHours(1);

    private final ImportFiles files;
    private final ImportJobs jobs;
    private final Clock clock;

    ImportFileSweeper(ImportFiles files, ImportJobs jobs, Clock clock) {
        this.files = files;
        this.jobs = jobs;
        this.clock = clock;
    }

    void sweep() {
        var stored = files.list();
        var statuses = jobs.statuses(stored.stream().map(ImportFiles.StoredFile::jobId).filter(Objects::nonNull).toList());
        for (var file : stored) {
            String status = file.jobId() == null ? null : statuses.get(file.jobId());
            boolean finished = "done".equals(status) || "failed".equals(status);
            boolean orphan = status == null && file.modifiedAt().plus(ORPHAN_AGE).isBefore(clock.instant());
            if (finished || orphan) {
                files.delete(file.fileName());
            }
        }
    }
}
