package app.salescopilot.importing;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import app.salescopilot.playbook.api.ImportActivity;

@Component
class ImportActivityAdapter implements ImportActivity {

    private final ImportJobs jobs;

    ImportActivityAdapter(ImportJobs jobs) {
        this.jobs = jobs;
    }

    @Override
    public boolean isActive(UUID playbookId) {
        return jobs.isActive(playbookId);
    }

    @Override
    public Optional<ImportView> latest(UUID playbookId) {
        return jobs.latest(playbookId);
    }
}
