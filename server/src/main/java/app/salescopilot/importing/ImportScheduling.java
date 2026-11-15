package app.salescopilot.importing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Os testes desligam o agendamento e chamam o worker e a varredura direto. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.import.scheduling-enabled", havingValue = "true")
class ImportScheduling {

    private final ImportWorker worker;
    private final ImportFileSweeper sweeper;

    ImportScheduling(ImportWorker worker, ImportFileSweeper sweeper) {
        this.worker = worker;
        this.sweeper = sweeper;
    }

    @Scheduled(fixedDelayString = "5s")
    void work() {
        worker.drain();
    }

    @Scheduled(fixedDelayString = "10m")
    void sweep() {
        sweeper.sweep();
    }
}
