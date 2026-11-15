package app.salescopilot.playbook.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O que o playbook precisa saber da importação, implementado pelo módulo `importing`. Fica aqui para
 * que o playbook não dependa do importing, que já depende do playbook.
 */
public interface ImportActivity {

    boolean isActive(UUID playbookId);

    Optional<ImportView> latest(UUID playbookId);

    record ImportView(UUID id, String status, Map<String, Integer> counts, int skipped, List<String> notices,
            String errorCode) {
    }
}
